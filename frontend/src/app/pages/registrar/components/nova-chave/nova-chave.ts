import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { concatMap, from } from 'rxjs';

import { Alerta } from '../../../../components/alerta/alerta';
import { ResultadoRegistrarRequest } from '../../../../models/resultado.model';
import { ChavesService } from '../../../../services/chaves.service';
import { ResultadosService } from '../../../../services/resultados.service';
import { mensagemErroApi } from '../../../../utils/erro-api';
import { novoEventoId } from '../../../../utils/evento-id';

@Component({
  selector: 'app-nova-chave',
  imports: [FormsModule, Alerta],
  templateUrl: './nova-chave.html',
  styleUrl: './nova-chave.css',
})
export class NovaChave {
  private readonly chavesService = inject(ChavesService);
  private readonly resultadosService = inject(ResultadosService);

  readonly modalidades = input.required<string[]>();
  readonly chaveCriada = output<string>();

  protected readonly nome = signal('');
  protected readonly modalidade = signal('FUTSAL');
  protected readonly equipes = signal('');
  protected readonly aviso = signal('');

  protected criar(): void {
    const equipes = this.equipes()
      .split(',')
      .map((e) => e.trim())
      .filter((e) => e.length > 0);
    this.aviso.set('');
    this.chavesService.incluir({ nome: this.nome(), modalidade: this.modalidade(), equipes }).subscribe({
      next: (chave) => {
        this.aviso.set(`Chave criada: ${chave.nome}, com ${chave.equipes.length} equipes.`);
        this.nome.set('');
        this.equipes.set('');
        this.chaveCriada.emit(chave.id);
      },
      error: (erro) => this.aviso.set(mensagemErroApi(erro)),
    });
  }

  protected carregarExemplo(): void {
    this.aviso.set('');
    this.chavesService.incluir({ nome: 'Chave A (exemplo)', modalidade: 'FUTSAL', equipes: ['IFCE', 'IFG', 'IFSP'] }).subscribe({
      next: (chave) => {
        const resultados: ResultadoRegistrarRequest[] = [
          { eventoId: novoEventoId(), partidaId: `${chave.id}-J1`, placarMandante: 4, placarVisitante: 1 },
          { eventoId: novoEventoId(), partidaId: `${chave.id}-J3`, placarMandante: 3, placarVisitante: 1 },
          { eventoId: novoEventoId(), partidaId: `${chave.id}-J2`, placarMandante: 3, placarVisitante: 4 },
        ];
        from(resultados)
          .pipe(concatMap((resultado) => this.resultadosService.registrar(resultado)))
          .subscribe({
            complete: () => {
              this.aviso.set('Exemplo carregado. Veja a tela Classificação.');
              this.chaveCriada.emit(chave.id);
            },
            error: (erro) => this.aviso.set(mensagemErroApi(erro)),
          });
      },
      error: (erro) => this.aviso.set(mensagemErroApi(erro)),
    });
  }
}
