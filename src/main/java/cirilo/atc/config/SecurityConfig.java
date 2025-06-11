//package cirilo.atc.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.firewall.StrictHttpFirewall;
//import org.springframework.web.cors.CorsConfiguration;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Value("${allowed.origin:chrome-extension://*}")
//    private String allowedOrigin;
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(cors -> cors.configurationSource(request -> {
//                    CorsConfiguration config = new CorsConfiguration();
//                    config.addAllowedOrigin(allowedOrigin);
//                    config.addAllowedMethod("*");
//                    config.addAllowedHeader("*");
//                    return config;
//                }))
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/translate").permitAll()
//                        .anyRequest().denyAll()
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public StrictHttpFirewall httpFirewall() {
//        StrictHttpFirewall firewall = new StrictHttpFirewall();
//        firewall.setAllowedHeaderNames(header -> true);
//        firewall.setAllowedParameterNames(param -> true);
//        return firewall;
//    }
//}