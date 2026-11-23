import { Component, input } from '@angular/core';

import { INTERVALO_ATUALIZACAO_MS } from '../../../../config/api';
import { ClassificacaoResponse } from '../../../../models/classificacao.model';
import { SaldoPipe } from '../../../../pipes/saldo.pipe';

@Component({
  selector: 'app-tabela-classificacao',
  imports: [SaldoPipe],
  templateUrl: './tabela-classificacao.html',
  styleUrl: './tabela-classificacao.css',
})
export class TabelaClassificacao {
  readonly tabela = input<ClassificacaoResponse | null>(null);

  protected readonly intervaloSegundos = INTERVALO_ATUALIZACAO_MS / 1000;
}
