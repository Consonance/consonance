package io.consonance.webservice.core;

import java.security.Principal;

/**
 * @author dyuen
 */
public class User implements Principal{
        public static final int RANDOM_CONSTANT = 100;
        private final String name;

        public User(String name) {
                this.name = name;
        }

        public String getName() {
                return name;
        }

        public int getId() {
                return (int) (Math.random() * RANDOM_CONSTANT);
        }
}
