"""
Observa o painel do cluster (GET /api/cluster) e mede o tempo de recuperação
depois da queda de um worker. Rodar em paralelo com o teste de falha.

  python3 chaos/observar_cluster.py --base http://localhost:8080 --intervalo 0.2

Para cada worker que sai de VIVO, registra:
  queda estimada  = instante da primeira leitura em que ele não está VIVO, menos o silêncio informado;
  detecção        = primeira leitura em que ele aparece como MORTO;
  reatribuição    = primeira leitura em que ele não tem mais chaves no anel.
Tempo de recuperação = reatribuição menos queda estimada. A precisão é a do intervalo de leitura.
Só usa a biblioteca padrão do Python.
"""
import argparse
import json
import time
import urllib.request


def ler(base):
    with urllib.request.urlopen(base + "/api/cluster", timeout=2) as resposta:
        return json.load(resposta)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:8080")
    parser.add_argument("--intervalo", type=float, default=0.2)
    parser.add_argument("--duracao", type=float, default=300)
    args = parser.parse_args()

    acompanhando = {}
    fim = time.time() + args.duracao
    print("instante; worker; evento; detalhe", flush=True)
    while time.time() < fim:
        agora = time.time()
        try:
            visao = ler(args.base)
        except Exception as erro:
            print(f"{agora:.3f}; -; gateway sem resposta; {erro}", flush=True)
            time.sleep(args.intervalo)
            continue
        for w in visao["workers"]:
            wid, estado, chaves = w["id"], w["estado"], w["chaves"]
            a = acompanhando.get(wid)
            if estado != "VIVO" and a is None:
                a = {"queda": agora - w["silencioMs"] / 1000.0, "chaves": len(chaves)}
                acompanhando[wid] = a
                print(f"{agora:.3f}; {wid}; saiu de VIVO; estado={estado}, silencio={w['silencioMs']} ms, "
                      f"chaves={len(chaves)}", flush=True)
            if a is None:
                continue
            if estado == "MORTO" and "deteccao" not in a:
                a["deteccao"] = agora
                print(f"{agora:.3f}; {wid}; declarado MORTO; {1000 * (agora - a['queda']):.0f} ms depois da queda",
                      flush=True)
            if estado != "VIVO" and not chaves and "reatribuicao" not in a:
                a["reatribuicao"] = agora
                print(f"{agora:.3f}; {wid}; chaves reatribuídas; recuperação = "
                      f"{1000 * (agora - a['queda']):.0f} ms (±{1000 * args.intervalo:.0f} ms), "
                      f"{a['chaves']} chaves mudaram de dono", flush=True)
            if estado == "VIVO" and "reatribuicao" in a:
                print(f"{agora:.3f}; {wid}; voltou; com {len(chaves)} chaves", flush=True)
                del acompanhando[wid]
        time.sleep(args.intervalo)


if __name__ == "__main__":
    main()
