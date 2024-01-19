package com.microservices.UserSerivce.service;

import com.microservices.UserSerivce.dao.Dao;
import com.microservices.UserSerivce.dto.TaskDto;
import com.microservices.UserSerivce.entity.User;
import com.microservices.UserSerivce.exception.CustomNotFoundException;
import com.microservices.UserSerivce.external.TaskService;
import com.microservices.UserSerivce.jwt.JwtUtils;
import com.microservices.UserSerivce.mapper.UserMapper;
import com.microservices.UserSerivce.repository.UserRepository;
import org.springframework.stereotype.*;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Service
public class UserService implements Dao<User> {
    private final UserRepository userRepository;
    TaskService taskService;
    UserMapper userMapper;
    RestTemplate restTemplate;
    JwtUtils jwtUtils;

    public UserService(UserRepository userRepository, TaskService taskService, UserMapper userMapper,
            RestTemplate restTemplate, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public User add(User o) {
        return userRepository.save(o);
    }

    @Override
    public User update(int id, User o) {
        return userRepository.findById(id).map((user) -> {
            user.setNom(o.getNom());
            user.setPrenom(o.getPrenom());
            user.setEmail(o.getEmail());
            user.setUsername(o.getUsername());
            user.setPassword(o.getPassword());
            return userRepository.save(user);
        }).orElseThrow(() -> new CustomNotFoundException("User not found with ID : " + id));
    }

    @Override
    public User delete(int id) {
        User temp = userRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("User not found with ID: " + id));
        userRepository.deleteById(id);
        return temp;
    }

    @Override
    public User findById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("User not found with ID: " + id));
    }

    @Override
    public List<User> findAll() {
        List<User> userList = userRepository.findAll();
        if (userList.isEmpty()) {
            throw new CustomNotFoundException("No users found");
        }
        return userList;
    }

    /*
     * public String addtask(String username, TaskDto taskDto) {
     * 
     * User u = userRepository.findByUsername(username)
     * .orElseThrow(() -> new
     * CustomNotFoundException("User not found with username: " + username));
     * HttpHeaders headers = new HttpHeaders();
     * headers.setBearerAuth(jwtUtils.generateToken(u.getUsername(), "USER"));
     * TaskDto test =
     * restTemplate.postForEntity("http://TASK-SERVICES/api/task/save", taskDto,
     * TaskDto.class).getBody();
     * u.getTasks().add(test.getId());
     * if (!u.getTasks().isEmpty()) {
     * userRepository.save(u);
     * return "inside if";
     * }
     * return "task added";
     * }
     */
    public User addtask(String username, TaskDto taskDto) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomNotFoundException("User not found with username: " + username));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtUtils.generateToken(u.getUsername(), "USER"));
        HttpEntity<TaskDto> entity = new HttpEntity<>(taskDto, headers);
        TaskDto test = restTemplate.postForEntity("http://TASK-SERVICES/api/task/save", entity,
                TaskDto.class).getBody();
        u.getTasks().add(test.getId());
        if (!u.getTasks().isEmpty()) {
            userRepository.save(u);
            return u;
        }
        return new User();
    }

    public String loadUsername(String username) {
        User userFound = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomNotFoundException("User not found with username: " + username));
        return userFound.getUsername();
    }

    public User loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomNotFoundException("User not found with username: " + username));
    }

    public List<TaskDto> getTasksForUser(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomNotFoundException("User not found with username: " + username));
        String jwtToken = jwtUtils.generateToken(u.getUsername(), "USER");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return u.getTasks().stream()
                .map(taskId -> {
                    ResponseEntity<TaskDto> response = restTemplate.exchange(
                            "http://TASK-SERVICES/api/task/" + taskId,
                            HttpMethod.GET,
                            entity,
                            TaskDto.class);
                    return response.getBody();
                })
                .collect(Collectors.toList());
    }

    public void deleteUserTask(String username, int idTask) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomNotFoundException("User not found with username: " + username));
        user.getTasks().removeIf(id -> id.equals(idTask));
        userRepository.save(user);
        String jwtToken = jwtUtils.generateToken(user.getUsername(), "USER");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange("http://TASK-SERVICES/api/task/delete/" + idTask, HttpMethod.DELETE, entity,
                String.class);
    }

    public TaskDto editUserTask(String username, int idTask, TaskDto editedTask) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomNotFoundException("User not found with username: " + username));
        String jwtToken = jwtUtils.generateToken(user.getUsername(), "USER");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<TaskDto> entity = new HttpEntity<>(editedTask, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                "http://TASK-SERVICES/api/task/update/" + idTask,
                HttpMethod.PUT,
                entity,
                Void.class);
        return editedTask;
    }
}