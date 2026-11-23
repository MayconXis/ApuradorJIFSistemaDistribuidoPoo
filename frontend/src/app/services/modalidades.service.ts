import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api';

@Injectable({
  providedIn: 'root'
})
export class ModalidadesService {

  private http = inject(HttpClient);

  private apiUrl = `${API_URL}/modalidades`;

  listar(): Observable<string[]> {
    return this.http.get<string[]>(this.apiUrl);
  }
}
