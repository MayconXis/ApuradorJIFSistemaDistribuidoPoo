import { SlicePipe } from '@angular/common';
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-mudancas-do-anel',
  imports: [SlicePipe],
  templateUrl: './mudancas-do-anel.html',
  styleUrl: './mudancas-do-anel.css',
})
export class MudancasDoAnel {
  readonly mudancas = input.required<string[]>();
}
