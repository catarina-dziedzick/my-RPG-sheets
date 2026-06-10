package com.myrpgsheets.repository;

import com.myrpgsheets.model.Campaign;
import com.myrpgsheets.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByDungeonMaster(User dungeonMaster);

    Optional<Campaign> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}