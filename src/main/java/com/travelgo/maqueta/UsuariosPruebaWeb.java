package com.travelgo.maqueta;

import com.travelgo.service.UsuarioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar la web, copia los usuarios de prueba de BackendConsola al UsuarioService de Spring, para que el login
 * web acepte los mismos correos que la consola (contraseña 123456). Sin esto no hay usuarios Agente ni Administrador
 * en la web, porque el registro solo crea clientes.
 * Son los mismos objetos Usuario, así sus reservas de ejemplo también aparecen en la web.
 * Se elimina cuando Integrante 4 cargue los usuarios de prueba en la base de datos.
 */
@Component
public class UsuariosPruebaWeb implements ApplicationRunner {

	private final UsuarioService usuarioService;

	public UsuariosPruebaWeb(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@Override
	public void run(ApplicationArguments args) {
		for (String correo : BackendConsola.CORREOS_EJEMPLO) {
			BackendConsola.USUARIOS.buscarPorCorreo(correo).ifPresent(usuarioService::guardar);
		}
	}

}
