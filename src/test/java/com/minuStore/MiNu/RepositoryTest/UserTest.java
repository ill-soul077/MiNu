package com.minuStore.MiNu.RepositoryTest;

import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.antlr.v4.runtime.tree.xpath.XPath.findAll;

@SpringBootTest
public class UserTest {
    @Autowired
    private UserRepository userRepository;
    @Test
    public void testUserRepository() {
        // Test findByUsername
        List<User>userList= userRepository.findAll();
        for(User user:userList){
            System.out.println(user.getUsername());
        }
    }
}
