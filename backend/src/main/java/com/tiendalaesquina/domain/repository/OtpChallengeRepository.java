package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.OtpChallenge;
import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.domain.model.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    @Modifying
    @Query("update OtpChallenge c set c.consumedAt = :now "
        + "where c.user = :user and c.purpose = :purpose and c.consumedAt is null")
    int invalidateActive(@Param("user") UserAccount user, @Param("purpose") OtpPurpose purpose,
                         @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.id = :id")
    Optional<OtpChallenge> findForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c "
        + "where c.user = :user and c.purpose = :purpose and c.consumedAt is null "
        + "order by c.createdAt desc")
    Optional<OtpChallenge> findLatestActiveForUpdate(@Param("user") UserAccount user,
                                                     @Param("purpose") OtpPurpose purpose);

    Optional<OtpChallenge> findTopByUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
        UserAccount user, OtpPurpose purpose);
}
