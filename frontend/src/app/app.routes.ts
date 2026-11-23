import { Routes } from '@angular/router';

import { Layout } from './components/layout/layout';
import { Classificacao } from './pages/classificacao/classificacao';
import { Cluster } from './pages/cluster/cluster';
import { Registrar } from './pages/registrar/registrar';

export const routes: Routes = [
  {
    path: '',
    component: Layout,
    children: [
      {
        path: '',
        redirectTo: 'registrar',
        pathMatch: 'full',
      },
      {
        path: 'registrar',
        component: Registrar,
        title: 'Registrar resultado',
      },
      {
        path: 'classificacao',
        component: Classificacao,
        title: 'Classificação',
      },
      {
        path: 'cluster',
        component: Cluster,
        title: 'Cluster',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'registrar',
  },
];
