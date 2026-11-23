import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api';
import { EventoResponse, ReciboResponse, ResultadoRegistrarRequest } from '../models/resultado.model';

@Injectable({
  providedIn: 'root'
})
export class ResultadosService {

  private http = inject(HttpClient);

  private apiUrl = API_URL;

  registrar(request: ResultadoRegistrarRequest): Observable<ReciboResponse> {
    return this.http.post<ReciboResponse>(`${this.apiUrl}/resultados`, request);
  }

  detalharEvento(eventoId: string): Observable<EventoResponse> {
    return this.http.get<EventoResponse>(`${this.apiUrl}/eventos/${encodeURIComponent(eventoId)}`);
  }
}
