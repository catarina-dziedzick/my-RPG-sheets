package com.myrpgsheets.repository;

import com.myrpgsheets.model.Campaign;
import com.myrpgsheets.model.CampaignCharacter;
import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignCharacterRepository extends JpaRepository<CampaignCharacter, Long> {

    List<CampaignCharacter> findByCampaignAndActiveTrue(Campaign campaign);

    List<CampaignCharacter> findByCampaignAndPlayerAndActiveTrue(Campaign campaign, User player);

    Optional<CampaignCharacter> findByCampaignAndCharacter(Campaign campaign, RpgCharacter character);

    boolean existsByCampaignAndCharacter(Campaign campaign, RpgCharacter character);

    List<CampaignCharacter> findByCharacterAndActiveTrue(RpgCharacter character);

    void deleteByCharacter(RpgCharacter character);
}