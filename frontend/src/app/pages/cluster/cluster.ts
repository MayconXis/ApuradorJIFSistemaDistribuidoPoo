import { Component, DestroyRef, inject, signal } from '@angular/core';

import { Alerta } from '../../components/alerta/alerta';
import { INTERVALO_ATUALIZACAO_MS } from '../../config/api';
import { ClusterResponse } from '../../models/cluster.model';
import { ClusterService } from '../../services/cluster.service';
import { mensagemErroApi } from '../../utils/erro-api';
import { ConferenciaDeConsistencia } from './components/conferencia-de-consistencia/conferencia-de-consistencia';
import { ListaDeWorkers } from './components/lista-de-workers/lista-de-workers';
import { MudancasDoAnel } from './components/mudancas-do-anel/mudancas-do-anel';
import { ResumoDoAnel } from './components/resumo-do-anel/resumo-do-anel';

@Component({
  selector: 'app-cluster',
  imports: [Alerta, ResumoDoAnel, ListaDeWorkers, MudancasDoAnel, ConferenciaDeConsistencia],
  templateUrl: './cluster.html',
})
export class Cluster {
  private readonly clusterService = inject(ClusterService);

  protected readonly cluster = signal<ClusterResponse | null>(null);
  protected readonly erro = signal('');

  constructor() {
    this.atualizar();
    const temporizador = setInterval(() => this.atualizar(), INTERVALO_ATUALIZACAO_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
  }

  private atualizar(): void {
    this.clusterService.detalhar().subscribe({
      next: (cluster) => {
        this.cluster.set(cluster);
        this.erro.set('');
      },
      error: (erro) => this.erro.set(mensagemErroApi(erro)),
    });
  }
}
