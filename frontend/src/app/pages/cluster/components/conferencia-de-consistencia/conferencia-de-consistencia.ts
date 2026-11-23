import { Component, inject, output, signal } from '@angular/core';

import { ConsistenciaResponse } from '../../../../models/cluster.model';
import { ClusterService } from '../../../../services/cluster.service';
import { mensagemErroApi } from '../../../../utils/erro-api';

@Component({
  selector: 'app-conferencia-de-consistencia',
  templateUrl: './conferencia-de-consistencia.html',
  styleUrl: './conferencia-de-consistencia.css',
})
export class ConferenciaDeConsistencia {
  private readonly clusterService = inject(ClusterService);

  readonly falhou = output<string>();

  protected readonly consistencia = signal<ConsistenciaResponse | null>(null);

  protected conferir(): void {
    this.clusterService.verificarConsistencia().subscribe({
      next: (consistencia) => this.consistencia.set(consistencia),
      error: (erro) => this.falhou.emit(mensagemErroApi(erro)),
    });
  }
}
