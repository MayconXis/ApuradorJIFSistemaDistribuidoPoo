import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'porcentagem',
})
export class PorcentagemPipe implements PipeTransform {

  transform(valor: number): string {
    return Math.round(valor * 100) + '%';
  }
}
