package com.mb.codegrip.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.Roles;
import com.mb.codegrip.dto.Users;

@Repository
public interface UserRepository extends CrudRepository<Users, Integer> {

	Optional<Users> findByEmail(String email);

	List<Users> findByIdIn(List<Integer> userIds);

	Boolean existsByEmail(String email);

	Optional<Users> findById(Integer id);

	public void save(org.springframework.security.core.userdetails.User user);

	Users findByGoogleIdToken(String googleIdToken);
	
	

	@Modifying
	@Transactional
	@Query("UPDATE Users u SET u.googleIdToken = :googleIdToken, u.provider = :provider WHERE u.id = :id")
	public void updateUser(@Param("id") Integer id, @Param("googleIdToken") String googleIdToken,
			@Param("provider") String provider);

	@Modifying
	@Transactional
	@Query("UPDATE Users u SET u.githubToken = :githubToken, u.profilePictureUrl = :profilePictureUrl,  u.provider = :provider, u.code = :code, u.githubLogin = :githubLogin WHERE u.id = :id")
	public void updateUserByGithub(@Param("id") int id, @Param("githubToken") String githubToken,
			@Param("profilePictureUrl") String profilePictureUrl, @Param("provider") String provider,
			@Param("code") String code, @Param("githubLogin") String githubLogin);

	Users findByGithubLoginOrEmail(String githubLogin, String email);

	Users findByBitbucketAccountId(String bitbucketAccountId);
	
		
	@Query(value="SELECT u.email,u.id  FROM Users u where u.email LIKE :email% and u.company_id= :companyId",nativeQuery=true)
	public List<Object[]> findWithEmailIdAndCompanyId(@Param("email") String email,@Param("companyId")Company companyId);

	Optional<Users> findByEmailAndIsDeleted(String email, boolean b);

	Optional<Users> findByEmailAndRoles(String username, Set<Roles> roles);

	
}	
