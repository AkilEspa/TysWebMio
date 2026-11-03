import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { Usuario } from './services/usuario';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        {
          provide: Usuario,
          useValue: {
            session$: of(null),
            sessionLoading$: of(false),
            autoLogin: () => of(null),
            confirmRegistration: () => of(''),
            logout: () => of(void 0),
          },
        },
      ],
    }).compileComponents();
  });

  it('renders the user portal shell', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.textContent).toContain('Rueda');
    expect(compiled.querySelector('footer')?.textContent).toContain('Alquiler de vehículos');
  });
});
