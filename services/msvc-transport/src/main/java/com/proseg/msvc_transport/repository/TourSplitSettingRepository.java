package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourSplitSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TourSplitSettingRepository extends JpaRepository<TourSplitSetting, UUID> {
}
