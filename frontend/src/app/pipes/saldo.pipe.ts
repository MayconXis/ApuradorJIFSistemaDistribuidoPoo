import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'saldo',
})
export class SaldoPipe implements PipeTransform {

  transform(valor: number): string {
    return valor > 0 ? '+' + valor : String(valor);
  }
}
