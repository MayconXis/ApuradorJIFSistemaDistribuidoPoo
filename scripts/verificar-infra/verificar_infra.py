"""
Verifica, em um RabbitMQ e um PostgreSQL de verdade, os quatro comportamentos
da infraestrutura em que o projeto se apoia. Não testa o código Java: testa
as garantias que o código Java assume.

  V1. RabbitMQ: mensagem não confirmada volta para a fila quando o consumidor cai.
  V2. RabbitMQ: fila quórum com limite de entregas manda a mensagem para a DLQ.
  V3. PostgreSQL: SELECT ... FOR UPDATE na linha da chave faz a segunda transação esperar.
  V4. PostgreSQL: evento_id repetido é recusado pela restrição UNIQUE.

Uso:
  pip install pika psycopg2-binary
  python3 verificar_infra.py   (com RabbitMQ em localhost:5672 e o banco do schema.sql em localhost:5432)
"""
import os
import sys
import threading
import time

import pika
import psycopg2

RABBIT = pika.ConnectionParameters(host=os.getenv("RABBIT_HOST", "localhost"),
                                   credentials=pika.PlainCredentials(os.getenv("RABBIT_USER", "guest"),
                                                                     os.getenv("RABBIT_PASSWORD", "guest")))
BANCO = dict(host=os.getenv("DB_HOST", "localhost"), dbname=os.getenv("DB_NAME", "apurador"),
             user=os.getenv("DB_USER", "apurador"), password=os.getenv("DB_PASSWORD", "apurador"))

resultados = []


def registrar(nome, passou, detalhe):
    resultados.append(passou)
    print(f"[{'PASSOU' if passou else 'FALHOU'}] {nome}: {detalhe}")


def canal_novo():
    conexao = pika.BlockingConnection(RABBIT)
    return conexao, conexao.channel()


def v1_reentrega_sem_ack():
    conexao, canal = canal_novo()
    canal.queue_delete("verif.v1")
    canal.queue_declare("verif.v1", durable=True, arguments={"x-queue-type": "quorum"})
    canal.basic_publish("", "verif.v1", b"chave-A", pika.BasicProperties(delivery_mode=2))
    conexao.close()

    # Consumidor 1 recebe e cai sem confirmar.
    conexao, canal = canal_novo()
    metodo, _, corpo = canal.basic_get("verif.v1", auto_ack=False)
    primeira = (metodo is not None, metodo.redelivered if metodo else None, corpo)
    conexao.close()

    # Consumidor 2 recebe a mesma mensagem, marcada como reentregue.
    time.sleep(0.5)
    conexao, canal = canal_novo()
    metodo, _, corpo = canal.basic_get("verif.v1", auto_ack=False)
    segunda = (metodo is not None, metodo.redelivered if metodo else None, corpo)
    if metodo:
        canal.basic_ack(metodo.delivery_tag)
    canal.queue_delete("verif.v1")
    conexao.close()
    passou = primeira[0] and segunda[0] and segunda[1] is True and segunda[2] == b"chave-A"
    registrar("V1 reentrega sem ack", passou,
              f"1a entrega redelivered={primeira[1]}, 2a entrega redelivered={segunda[1]}, corpo={segunda[2]!r}")


def v2_limite_de_entregas_e_dlq():
    conexao, canal = canal_novo()
    canal.exchange_declare("verif.dlx", "fanout", durable=True)
    canal.queue_delete("verif.dlq")
    canal.queue_delete("verif.v2")
    canal.queue_declare("verif.dlq", durable=True, arguments={"x-queue-type": "quorum"})
    canal.queue_bind("verif.dlq", "verif.dlx")
    limite = 2
    canal.queue_declare("verif.v2", durable=True, arguments={
        "x-queue-type": "quorum", "x-delivery-limit": limite, "x-dead-letter-exchange": "verif.dlx"})
    canal.basic_publish("", "verif.v2", b"chave-B", pika.BasicProperties(delivery_mode=2))

    entregas = 0
    for _ in range(limite + 3):
        metodo, _, _ = canal.basic_get("verif.v2", auto_ack=False)
        if metodo is None:
            time.sleep(0.3)
            continue
        entregas += 1
        canal.basic_nack(metodo.delivery_tag, requeue=True)
    time.sleep(0.5)
    na_fila = canal.queue_declare("verif.v2", passive=True).method.message_count
    na_dlq = canal.queue_declare("verif.dlq", passive=True).method.message_count
    canal.queue_delete("verif.v2")
    canal.queue_delete("verif.dlq")
    canal.exchange_delete("verif.dlx")
    conexao.close()
    registrar("V2 limite de entregas e DLQ", na_fila == 0 and na_dlq == 1,
              f"x-delivery-limit={limite}, entregas antes de sair={entregas}, na fila={na_fila}, na DLQ={na_dlq}")


def v3_trava_da_chave():
    preparo = psycopg2.connect(**BANCO)
    preparo.autocommit = True
    with preparo.cursor() as c:
        c.execute("INSERT INTO chave VALUES ('verif-trava','Verificação','FUTSAL',0,now(),now()) ON CONFLICT DO NOTHING")
    espera = {}

    def segunda_transacao():
        conexao = psycopg2.connect(**BANCO)
        inicio = time.monotonic()
        with conexao.cursor() as c:
            c.execute("SELECT id FROM chave WHERE id = 'verif-trava' FOR UPDATE")
        espera["ms"] = (time.monotonic() - inicio) * 1000
        conexao.rollback()
        conexao.close()

    primeira = psycopg2.connect(**BANCO)
    with primeira.cursor() as c:
        c.execute("SELECT id FROM chave WHERE id = 'verif-trava' FOR UPDATE")
        outra = threading.Thread(target=segunda_transacao)
        outra.start()
        time.sleep(1.5)  # a primeira transação segura a trava por 1,5 s
        primeira.commit()
    outra.join()
    primeira.close()
    with preparo.cursor() as c:
        c.execute("DELETE FROM chave WHERE id = 'verif-trava'")
    preparo.close()
    registrar("V3 trava da chave", espera["ms"] >= 1400,
              f"a segunda transação esperou {espera['ms']:.0f} ms pela primeira (que segurou a trava por 1500 ms)")


def v4_evento_repetido():
    conexao = psycopg2.connect(**BANCO)
    conexao.autocommit = True
    recusou = False
    with conexao.cursor() as c:
        c.execute("INSERT INTO chave VALUES ('verif-unico','Verificação','FUTSAL',0,now(),now()) ON CONFLICT DO NOTHING")
        sql = ("INSERT INTO evento_resultado (seq, evento_id, chave_id, partida_id, estado, recebido_em) "
               "VALUES (nextval('evento_seq'), 'verif-evento', 'verif-unico', 'verif-unico-J1', 'PENDENTE', now())")
        c.execute(sql)
        try:
            c.execute(sql)
        except psycopg2.errors.UniqueViolation:
            recusou = True
        c.execute("SELECT count(*) FROM evento_resultado WHERE evento_id = 'verif-evento'")
        linhas = c.fetchone()[0]
        c.execute("DELETE FROM evento_resultado WHERE chave_id = 'verif-unico'")
        c.execute("DELETE FROM chave WHERE id = 'verif-unico'")
    conexao.close()
    registrar("V4 evento repetido", recusou and linhas == 1,
              f"segundo INSERT recusado={recusou}, linhas com o mesmo evento_id={linhas}")


if __name__ == "__main__":
    print("Verificação da infraestrutura do Apurador JIF")
    print(time.strftime("%Y-%m-%d %H:%M:%S %Z"))
    for verificacao in (v1_reentrega_sem_ack, v2_limite_de_entregas_e_dlq, v3_trava_da_chave, v4_evento_repetido):
        try:
            verificacao()
        except Exception as erro:  # registra e segue para a próxima
            registrar(verificacao.__name__, False, repr(erro))
    print(f"{sum(resultados)} de {len(resultados)} verificações passaram")
    sys.exit(0 if all(resultados) else 1)
