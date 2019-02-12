package com.mb.codegrip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.dto.Notification;
import com.mb.codegrip.model.NotificationModel;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer>{
	
	List<Notification> findByReceiverIdAndCompanyId(Integer receiverId, Integer companyId);
	
	@Modifying
	@Transactional
	@Query("UPDATE Notification n SET n.status = :isRead, n.isDeleted = :isDeleted  WHERE n.id = :id")
	public void updateNotificationFlag(@Param("id") Integer id, @Param("isRead") String status, @Param("isDeleted") Boolean isDeleted);

	void save(NotificationModel createFreshNotificationModel);

	Notification findById(Integer id);

}
