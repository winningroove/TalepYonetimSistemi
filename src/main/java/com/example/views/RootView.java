package com.example.views;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("")
@AnonymousAllowed
public class RootView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() 
                || auth.getPrincipal().equals("anonymousUser")) {
            event.forwardTo(LoginView.class);
            return;
        }

        String role = auth.getAuthorities().iterator().next().getAuthority();
        switch (role) {
             case "ROLE_CUSTOMER"       -> event.forwardTo("customer");
             case "ROLE_PRODUCT_OWNER"  -> event.forwardTo("po");
             case "ROLE_DEVELOPER"      -> event.forwardTo("developer");
             case "ROLE_ADMIN"          -> event.forwardTo("admin");
             case "ROLE_SCRUM_MASTER"   -> event.forwardTo("scrum-master");
             default                    -> event.forwardTo(LoginView.class);
}
    }
}