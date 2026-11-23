import { Component, input, output } from '@angular/core';

import { Alerta } from '../../../../components/alerta/alerta';
import { AuditoriaResponse } from '../../../../models/classificacao.model';

@Component({
  selector: 'app-auditoria-chave',
  imports: [Alerta],
  templateUrl: './auditoria-chave.html',
  styleUrl: './auditoria-chave.css',
})
export class AuditoriaChave {
  readonly auditoria = input<AuditoriaResponse | null>(null);
  readonly auditar = output<void>();
}
