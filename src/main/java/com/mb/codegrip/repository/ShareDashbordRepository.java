package com.mb.codegrip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.dto.ShareDashboard;

@Repository
public interface ShareDashbordRepository extends JpaRepository<ShareDashboard, Integer> {

	@Query(value = "SELECT distinct(s.receiver_mail_id),s.receiver_id from share_dashboard s where s.sender_id=:senderId and s.project_id =:projectId", nativeQuery = true)
	public List<Object[]> findbyProjectIdAndSenderId(@Param("senderId") int senderId,
			@Param("projectId") int projectId);

	@Modifying
	@Transactional
	@Query("UPDATE ShareDashboard s SET s.isDeleted = true WHERE s.receiverMailId = :receiverMailId and s.projectId=:projectId and s.senderId=:senderId")
	public void deleteSharedDashboardUsers(@Param("receiverMailId") String receiverMailId,
			@Param("projectId") Integer projectId, @Param("senderId") Integer senderId);
}
