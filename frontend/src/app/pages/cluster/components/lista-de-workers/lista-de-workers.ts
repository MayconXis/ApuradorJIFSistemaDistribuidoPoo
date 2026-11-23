import { Component, input } from '@angular/core';

import { ClusterResponse } from '../../../../models/cluster.model';
import { SegundosPipe } from '../../../../pipes/segundos.pipe';

@Component({
  selector: 'app-lista-de-workers',
  imports: [SegundosPipe],
  templateUrl: './lista-de-workers.html',
  styleUrl: './lista-de-workers.css',
})
export class ListaDeWorkers {
  readonly cluster = input.required<ClusterResponse>();

  protected classeDoEstado(estado: string): string {
    if (estado === 'VIVO') {
      return 'ok';
    }
    return estado === 'SUSPEITO' ? 'warn' : 'bad';
  }
}
