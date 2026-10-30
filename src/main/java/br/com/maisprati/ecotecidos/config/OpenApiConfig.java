package br.com.maisprati.ecotecidos.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public OpenAPI ecoTecidosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ecotecidos API")
                        .version("0.0.1")
                        .description("""
                                API REST do projeto **Ecotecidos**, desenvolvido como projeto de conclusão do curso de Desenvolvedor Full Stack da +PraTi.

                                A plataforma conecta doadores de tecidos a pontos de coleta, permitindo:
                                - cadastro e autenticação de usuários (JWT);
                                - consulta de pontos de coleta e busca por proximidade;
                                - registro e aprovação de descartes de tecidos, com geração de cashback;
                                - consulta de conteúdo educativo (posts e categorias);
                                - administração de usuários, pontos de coleta, conteúdo e regras de cashback.

                                **Autenticação:** realize o login em `POST /auth/login`, copie o token retornado e informe-o \
                                no botão **Authorize** (esquema `bearerAuth`). Os endpoints de `/auth/**` e `/register` são públicos; \
                                os demais exigem token JWT.

                                **Erros:** respostas de erro seguem o formato `ErrorResponseDTO` \
                                (`timestamp`, `status`, `error`, `message`, `path`). Erros de validação incluem também `fieldErrors`.
                                """)
                        .contact(new Contact()
                                .name("Equipe Ecotecidos")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório do Projeto - GitHub")
                        .url("https://github.com/lucastnsoares/ecotecidos-api"));
    }
}
