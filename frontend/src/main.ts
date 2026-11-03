import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { LOCALE_ID } from '@angular/core';
import { AppShellComponent } from './app/app-shell.component';

registerLocaleData(localeEsAr);
bootstrapApplication(AppShellComponent, { providers: [provideHttpClient(), { provide: LOCALE_ID, useValue: 'es-AR' }] })
  .catch(error => console.error(error));
