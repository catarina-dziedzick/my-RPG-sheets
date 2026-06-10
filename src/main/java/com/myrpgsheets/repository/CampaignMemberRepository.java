package com.myrpgsheets.repository;

import com.myrpgsheets.model.Campaign;
import com.myrpgsheets.model.CampaignMember;
import com.myrpgsheets.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignMemberRepository extends JpaRepository<CampaignMember, Long> {

    List<CampaignMember> findByCampaign(Campaign campaign);

    List<CampaignMember> findByUser(User user);

    Optional<CampaignMember> findByCampaignAndUser(Campaign campaign, User user);

    boolean existsByCampaignAndUser(Campaign campaign, User user);
}