import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api';
import { ClusterResponse, ConsistenciaResponse } from '../models/cluster.model';

@Injectable({
  providedIn: 'root'
})
export class ClusterService {

  private http = inject(HttpClient);

  private apiUrl = API_URL;

  detalhar(): Observable<ClusterResponse> {
    return this.http.get<ClusterResponse>(`${this.apiUrl}/cluster`);
  }

  verificarConsistencia(): Observable<ConsistenciaResponse> {
    return this.http.get<ConsistenciaResponse>(`${this.apiUrl}/consistencia`);
  }
}
