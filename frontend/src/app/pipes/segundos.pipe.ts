import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'segundos',
})
export class SegundosPipe implements PipeTransform {

  transform(ms: number): string {
    return (ms / 1000).toFixed(1).replace('.', ',') + ' s';
  }
}
