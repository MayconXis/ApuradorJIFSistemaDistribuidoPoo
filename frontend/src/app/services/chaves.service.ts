import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api';
import { ChaveIncluirRequest, ChaveResponse } from '../models/chave.model';
import { AuditoriaResponse, ClassificacaoResponse } from '../models/classificacao.model';
import { PartidaResponse } from '../models/partida.model';

@Injectable({
  providedIn: 'root'
})
export class ChavesService {

  private http = inject(HttpClient);

  private apiUrl = `${API_URL}/chaves`;

  listar(): Observable<ChaveResponse[]> {
    return this.http.get<ChaveResponse[]>(this.apiUrl);
  }

  incluir(request: ChaveIncluirRequest): Observable<ChaveResponse> {
    return this.http.post<ChaveResponse>(this.apiUrl, request);
  }

  listarPartidas(chaveId: string): Observable<PartidaResponse[]> {
    return this.http.get<PartidaResponse[]>(`${this.apiUrl}/${encodeURIComponent(chaveId)}/partidas`);
  }

  detalharClassificacao(chaveId: string, versaoAtual: number | null): Observable<HttpResponse<ClassificacaoResponse>> {
    let headers = new HttpHeaders();
    if (versaoAtual !== null) {
      headers = headers.set('If-None-Match', `"${versaoAtual}"`);
    }
    return this.http.get<ClassificacaoResponse>(`${this.apiUrl}/${encodeURIComponent(chaveId)}/classificacao`, {
      headers,
      observe: 'response',
    });
  }

  auditar(chaveId: string): Observable<AuditoriaResponse> {
    return this.http.get<AuditoriaResponse>(`${this.apiUrl}/${encodeURIComponent(chaveId)}/auditoria`);
  }
}
