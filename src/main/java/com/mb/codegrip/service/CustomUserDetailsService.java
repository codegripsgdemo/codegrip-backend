package com.mb.codegrip.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.mb.codegrip.dto.Roles;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.repository.UserRepository;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:notifications.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class CustomUserDetailsService implements UserDetailsService {

	private static final Logger LOGGER = Logger.getLogger(CustomUserDetailsService.class);
	private UserRepository userRepository;

	@Autowired
	public CustomUserDetailsService(UserRepository accountRepository) {
		this.userRepository = accountRepository;
	}

	/*********************************************************************************************
	 * This method loads when program gets executed.
	 ********************************************************************************************/
	@Override
	public UserDetails loadUserByUsername(String username) {

		Optional<Users> user = userRepository.findByEmailAndIsDeleted(username, false);

		if (user.isPresent()) {
			List<GrantedAuthority> authorities = buildUserAuthority(user.get().getRoles());

			return buildUserForAuthentication(user.get(), authorities);
		} else {
			return null;
		}

	}

	/*********************************************************************************************
	 * Build use authority.
	 ********************************************************************************************/
	private List<GrantedAuthority> buildUserAuthority(Set<Roles> userRoles) {
		Set<GrantedAuthority> setAuths = new HashSet<>();
		for (Roles userRole : userRoles) {
			LOGGER.info("called buildUserAuthority(Set<UserRole> userRoles) method.....");
			setAuths.add(new SimpleGrantedAuthority(userRole.getName()));
		}
		return new ArrayList<>(setAuths);
	}

	/*********************************************************************************************
	 * Builder for authentication.
	 ********************************************************************************************/
	private User buildUserForAuthentication(Users user, List<GrantedAuthority> authorities) {
		// accountNonExpired, credentialsNonExpired, accountNonLocked, authorities
		// properties
		LOGGER.info("called buildUserForAuthentication(Users user, List<GrantedAuthority> authorities) method....");
		return new User(user.getEmail(), user.getPassword(), true, true, true, true, authorities);
	}

	/*********************************************************************************************
	 * Function to save user
	 ********************************************************************************************/
	@Transactional
	public Users saveUser(Users user) {
		LOGGER.info("<------------  Sign Up User Service Start ---------------->");
		return userRepository.save(user);
	}

}
