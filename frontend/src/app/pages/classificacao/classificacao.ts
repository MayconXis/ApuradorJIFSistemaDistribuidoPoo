import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Alerta } from '../../components/alerta/alerta';
import { INTERVALO_ATUALIZACAO_MS } from '../../config/api';
import { ChaveResponse } from '../../models/chave.model';
import { AuditoriaResponse, ClassificacaoResponse } from '../../models/classificacao.model';
import { PartidaResponse } from '../../models/partida.model';
import { ChavesService } from '../../services/chaves.service';
import { mensagemErroApi } from '../../utils/erro-api';
import { AuditoriaChave } from './components/auditoria-chave/auditoria-chave';
import { ListaPartidas } from './components/lista-partidas/lista-partidas';
import { TabelaClassificacao } from './components/tabela-classificacao/tabela-classificacao';

@Component({
  selector: 'app-classificacao',
  imports: [FormsModule, Alerta, TabelaClassificacao, ListaPartidas, AuditoriaChave],
  templateUrl: './classificacao.html',
})
export class Classificacao {
  private readonly chavesService = inject(ChavesService);

  protected readonly chaves = signal<ChaveResponse[]>([]);
  protected readonly chaveId = signal('');
  protected readonly tabela = signal<ClassificacaoResponse | null>(null);
  protected readonly partidas = signal<PartidaResponse[]>([]);
  protected readonly auditoria = signal<AuditoriaResponse | null>(null);
  protected readonly erro = signal('');

  constructor() {
    this.chavesService.listar().subscribe({
      next: (lista) => {
        this.chaves.set(lista);
        if (lista.length > 0) {
          this.escolherChave(lista[lista.length - 1].id);
        }
      },
      error: (erro) => this.erro.set(mensagemErroApi(erro)),
    });
    const temporizador = setInterval(() => this.atualizar(), INTERVALO_ATUALIZACAO_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
  }

  protected escolherChave(id: string): void {
    this.chaveId.set(id);
    this.tabela.set(null);
    this.auditoria.set(null);
    this.atualizar();
  }

  protected auditar(): void {
    const id = this.chaveId();
    if (id) {
      this.chavesService.auditar(id).subscribe({
        next: (auditoria) => this.auditoria.set(auditoria),
        error: (erro) => this.erro.set(mensagemErroApi(erro)),
      });
    }
  }

  private atualizar(): void {
    const id = this.chaveId();
    if (!id) {
      return;
    }
    const atual = this.tabela();
    this.chavesService.detalharClassificacao(id, atual && atual.chaveId === id ? atual.versao : null).subscribe({
      next: (resposta) => {
        if (resposta.body && this.chaveId() === id) {
          this.tabela.set(resposta.body);
          this.erro.set('');
          this.chavesService.listarPartidas(id).subscribe({
            next: (lista) => this.partidas.set(lista),
            error: () => {},
          });
        }
      },
      error: (erro) => {
        if (erro?.status !== 304) {
          this.erro.set(mensagemErroApi(erro));
        }
      },
    });
  }
}
