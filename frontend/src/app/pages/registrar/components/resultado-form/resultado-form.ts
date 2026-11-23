import { Component, DestroyRef, computed, inject, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Alerta } from '../../../../components/alerta/alerta';
import { ChaveResponse } from '../../../../models/chave.model';
import { PartidaResponse } from '../../../../models/partida.model';
import { EventoResponse, ReciboResponse, ResultadoRegistrarRequest } from '../../../../models/resultado.model';
import { ResultadosService } from '../../../../services/resultados.service';
import { INTERVALO_EVENTO_MS } from '../../../../config/api';
import { mensagemErroApi } from '../../../../utils/erro-api';
import { novoEventoId } from '../../../../utils/evento-id';

@Component({
  selector: 'app-resultado-form',
  imports: [FormsModule, Alerta],
  templateUrl: './resultado-form.html',
  styleUrl: './resultado-form.css',
})
export class ResultadoForm {
  private readonly resultadosService = inject(ResultadosService);
  private temporizador: ReturnType<typeof setInterval> | null = null;

  readonly chaves = input.required<ChaveResponse[]>();
  readonly partidas = input.required<PartidaResponse[]>();
  readonly chaveId = input.required<string>();
  readonly partidaId = model('');
  readonly erro = model('');
  readonly chaveEscolhida = output<string>();
  readonly resultadoAplicado = output<void>();

  protected readonly placarMandante = signal<number | null>(0);
  protected readonly placarVisitante = signal<number | null>(0);
  protected readonly sets = signal<(number | null)[][]>([[null, null], [null, null], [null, null]]);
  protected readonly wo = signal(false);
  protected readonly ausente = signal<'MANDANTE' | 'VISITANTE'>('VISITANTE');
  protected readonly amarelosMandante = signal<number | null>(0);
  protected readonly amarelosVisitante = signal<number | null>(0);
  protected readonly vermelhosMandante = signal<number | null>(0);
  protected readonly vermelhosVisitante = signal<number | null>(0);
  protected readonly sancoesMandante = signal<number | null>(0);
  protected readonly sancoesVisitante = signal<number | null>(0);

  protected readonly enviando = signal(false);
  protected readonly recibo = signal<ReciboResponse | null>(null);
  protected readonly evento = signal<EventoResponse | null>(null);

  protected readonly chaveAtual = computed(() => this.chaves().find((c) => c.id === this.chaveId()) ?? null);
  protected readonly partidaAtual = computed(() => this.partidas().find((p) => p.id === this.partidaId()) ?? null);
  protected readonly modalidade = computed(() => this.chaveAtual()?.modalidade ?? '');
  protected readonly ehVolei = computed(() => this.modalidade() === 'VOLEIBOL');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.pararDeAcompanhar());
  }

  protected mudarSet(indice: number, lado: number, valor: string): void {
    const copia = this.sets().map((s) => [...s]);
    copia[indice][lado] = valor === '' ? null : Number(valor);
    this.sets.set(copia);
  }

  protected registrar(): void {
    const partida = this.partidaAtual();
    if (!partida) {
      this.erro.set('Escolha a partida.');
      return;
    }
    const request: ResultadoRegistrarRequest = { eventoId: novoEventoId(), partidaId: partida.id };
    if (this.wo()) {
      request.wo = true;
      request.ausente = this.ausente();
    } else if (this.ehVolei()) {
      request.parciais = this.sets()
        .filter((s) => s[0] !== null && s[1] !== null)
        .map((s) => [s[0] as number, s[1] as number]);
    } else {
      request.placarMandante = this.placarMandante() ?? 0;
      request.placarVisitante = this.placarVisitante() ?? 0;
      request.amarelosMandante = this.amarelosMandante() ?? 0;
      request.amarelosVisitante = this.amarelosVisitante() ?? 0;
      request.vermelhosMandante = this.vermelhosMandante() ?? 0;
      request.vermelhosVisitante = this.vermelhosVisitante() ?? 0;
      request.sancoesMandante = this.sancoesMandante() ?? 0;
      request.sancoesVisitante = this.sancoesVisitante() ?? 0;
    }
    this.enviando.set(true);
    this.erro.set('');
    this.recibo.set(null);
    this.evento.set(null);
    this.resultadosService.registrar(request).subscribe({
      next: (recibo) => {
        this.enviando.set(false);
        this.recibo.set(recibo);
        this.acompanhar(recibo.eventoId);
      },
      error: (erro) => {
        this.enviando.set(false);
        this.erro.set(mensagemErroApi(erro));
      },
    });
  }

  protected classeDoEstado(estado: string): string {
    if (estado === 'APLICADO' || estado === 'HOMOLOGADA') {
      return 'ok';
    }
    if (estado === 'REJEITADO') {
      return 'bad';
    }
    return 'warn';
  }

  private acompanhar(eventoId: string): void {
    this.pararDeAcompanhar();
    const consultar = () =>
      this.resultadosService.detalharEvento(eventoId).subscribe({
        next: (evento) => {
          this.evento.set(evento);
          if (evento.estado !== 'PENDENTE') {
            this.pararDeAcompanhar();
            this.resultadoAplicado.emit();
          }
        },
        error: () => {},
      });
    consultar();
    this.temporizador = setInterval(consultar, INTERVALO_EVENTO_MS);
  }

  private pararDeAcompanhar(): void {
    if (this.temporizador !== null) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }
}
