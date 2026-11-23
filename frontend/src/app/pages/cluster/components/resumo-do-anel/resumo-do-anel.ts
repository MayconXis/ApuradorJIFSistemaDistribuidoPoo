import { Component, computed, input } from '@angular/core';

import { ClusterResponse } from '../../../../models/cluster.model';
import { PorcentagemPipe } from '../../../../pipes/porcentagem.pipe';
import { SegundosPipe } from '../../../../pipes/segundos.pipe';

@Component({
  selector: 'app-resumo-do-anel',
  imports: [PorcentagemPipe, SegundosPipe],
  templateUrl: './resumo-do-anel.html',
  styleUrl: './resumo-do-anel.css',
})
export class ResumoDoAnel {
  readonly cluster = input.required<ClusterResponse>();

  protected readonly vivos = computed(() => this.cluster().workers.filter((w) => w.estado !== 'MORTO').length);
}
