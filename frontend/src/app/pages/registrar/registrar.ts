import { Component, inject, signal } from '@angular/core';

import { ChaveResponse } from '../../models/chave.model';
import { PartidaResponse } from '../../models/partida.model';
import { ChavesService } from '../../services/chaves.service';
import { ModalidadesService } from '../../services/modalidades.service';
import { mensagemErroApi } from '../../utils/erro-api';
import { NovaChave } from './components/nova-chave/nova-chave';
import { ResultadoForm } from './components/resultado-form/resultado-form';

@Component({
  selector: 'app-registrar',
  imports: [ResultadoForm, NovaChave],
  templateUrl: './registrar.html',
})
export class Registrar {
  private readonly chavesService = inject(ChavesService);
  private readonly modalidadesService = inject(ModalidadesService);

  protected readonly chaves = signal<ChaveResponse[]>([]);
  protected readonly partidas = signal<PartidaResponse[]>([]);
  protected readonly modalidades = signal<string[]>([]);
  protected readonly chaveId = signal('');
  protected readonly partidaId = signal('');
  protected readonly erro = signal('');

  constructor() {
    this.carregarChaves();
    this.modalidadesService.listar().subscribe({
      next: (modalidades) => this.modalidades.set(modalidades),
      error: () => {},
    });
  }

  protected carregarChaves(selecionar?: string): void {
    this.chavesService.listar().subscribe({
      next: (lista) => {
        this.chaves.set(lista);
        const alvo = selecionar ?? (this.chaveId() || lista[0]?.id || '');
        if (alvo) {
          this.escolherChave(alvo);
        }
      },
      error: (erro) => this.erro.set(mensagemErroApi(erro)),
    });
  }

  protected escolherChave(id: string): void {
    this.chaveId.set(id);
    this.partidaId.set('');
    this.chavesService.listarPartidas(id).subscribe({
      next: (lista) => {
        this.partidas.set(lista);
        const semResultado = lista.find((p) => p.estado === 'AGENDADA') ?? lista[0];
        this.partidaId.set(semResultado?.id ?? '');
      },
      error: (erro) => this.erro.set(mensagemErroApi(erro)),
    });
  }
}
