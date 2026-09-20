package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IHabitacionRepository;
import com.example.sistemahotel.repositories.IRegistroRepository;

// Cubre el bug real encontrado en produccion: extenderEstadia() cambiaba las
// noches pero nunca recalculaba la tarifa, dejando la estadia cobrada al
// precio original sin importar cuanto se extendiera.
@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock
    private IRegistroRepository repositorio;

    @Mock
    private IHabitacionRepository habitacionrepositorio;

    private RegistroService service;

    private Registro registro;

    @BeforeEach
    void setUp() {
        service = new RegistroService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
        ReflectionTestUtils.setField(service, "habitacionrepositorio", habitacionrepositorio);

        Categoria categoria = new Categoria();
        categoria.setNombre("Habitación Estándar");
        categoria.setPrecio(150.0);

        Habitacion habitacion = new Habitacion();
        habitacion.setId(101);
        habitacion.setNumero("101");
        habitacion.setCategoria(categoria);

        registro = new Registro();
        registro.setId(6);
        registro.setHabitacion(habitacion);
        registro.setCheckIn(fecha(2026, Calendar.AUGUST, 27));
        registro.setNoches(10);
        registro.setTarifa(150.0);
        registro.setEstado(EstadoRegistro.ACTIVA);
    }

    private Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes, dia, 13, 29, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    void extenderEstadiaRecalculaLaTarifaConLasNochesNuevas() {
        when(repositorio.findById(6)).thenReturn(Optional.of(registro));
        when(repositorio.findAll()).thenReturn(List.of()); // sin otros registros activos: no hay solapamiento

        boolean ok = service.extenderEstadia(6, 12);

        assertTrue(ok);

        ArgumentCaptor<Registro> captor = ArgumentCaptor.forClass(Registro.class);
        verify(repositorio).save(captor.capture());

        Registro guardado = captor.getValue();
        assertEquals(12, guardado.getNoches());
        assertEquals(1800.0, guardado.getTarifa(), 0.001); // 150/noche * 12 noches, no 150 plano
    }

    @Test
    void extenderEstadiaNoTocaNadaSiHaySolapamiento() {
        Registro otroRegistro = new Registro();
        otroRegistro.setId(99);
        otroRegistro.setHabitacion(registro.getHabitacion());
        otroRegistro.setCheckIn(fecha(2026, Calendar.SEPTEMBER, 1));
        otroRegistro.setNoches(3);
        otroRegistro.setEstado(EstadoRegistro.ACTIVA);

        when(repositorio.findById(6)).thenReturn(Optional.of(registro));
        when(repositorio.findAll()).thenReturn(List.of(registro, otroRegistro));

        boolean ok = service.extenderEstadia(6, 12);

        assertTrue(!ok);
        verify(repositorio, org.mockito.Mockito.never()).save(any());
    }
}
