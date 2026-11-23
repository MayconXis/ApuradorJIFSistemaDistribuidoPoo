import { Component, input } from '@angular/core';

import { PartidaResponse } from '../../../../models/partida.model';

@Component({
  selector: 'app-lista-partidas',
  templateUrl: './lista-partidas.html',
  styleUrl: './lista-partidas.css',
})
export class ListaPartidas {
  readonly partidas = input.required<PartidaResponse[]>();

  protected placar(partida: PartidaResponse): string {
    if (partida.estado === 'AGENDADA') {
      return 'sem resultado';
    }
    const base = `${partida.placarMandante} x ${partida.placarVisitante}`;
    if (partida.wo) {
      return base + ' (WO)';
    }
    return partida.parciais ? `${base} (${partida.parciais})` : base;
  }
}
