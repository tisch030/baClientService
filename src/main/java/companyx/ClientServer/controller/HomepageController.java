package companyx.ClientServer.controller;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomepageController {

    @NonNull
    private final WebClient webClient;

    @NonNull
    @GetMapping("/")
    public String root() {
        return "redirect:/index";
    }

    @NonNull
    @GetMapping("/index")
    public String index(@Nullable final Principal principal,
                        @NonNull final Model model) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
            model.addAttribute("activities", ((OAuth2AuthenticationToken) principal).getPrincipal().getAttribute("activities"));
            model.addAttribute("modules", ((OAuth2AuthenticationToken) principal).getPrincipal().getAttribute("modules"));
        }
        return "index";
    }

    @GetMapping("login")
    public String login() {
        return "login";
    }


    @GetMapping("/single-logout")
    public String logout(@NonNull final HttpServletRequest request,
                         @NonNull final HttpServletResponse response,
                         @NonNull final Authentication authentication) {

        final String xsrfCookieName = "XSRF-TOKEN";
        final List<Cookie> cookies = Arrays.stream(request.getCookies()).toList();
        final Cookie xsrfCookie = cookies.stream()
                .filter(cookie -> xsrfCookieName.equals(cookie.getName()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);

        final String authSessionCookieName = "AUTHSESSIONID";
        final Cookie authSessionCookie = cookies.stream()
                .filter(cookie -> authSessionCookieName.equals(cookie.getName()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);

        this.webClient
                .post()
                .uri("http://127.0.0.1:9500/api/auth/logout")
                .cookie(xsrfCookieName, xsrfCookie.getValue())
                .cookie(authSessionCookieName, authSessionCookie.getValue())
                .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                .retrieve()
                .toEntity(Void.class)
                .block();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:login";
    }

}
