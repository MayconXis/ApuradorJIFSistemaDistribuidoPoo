import { Component, input } from '@angular/core';

@Component({
  selector: 'app-alerta',
  templateUrl: './alerta.html',
  styleUrl: './alerta.css',
})
export class Alerta {
  readonly tipo = input<'ok' | 'bad' | 'warn' | 'neutro'>('neutro');
}
