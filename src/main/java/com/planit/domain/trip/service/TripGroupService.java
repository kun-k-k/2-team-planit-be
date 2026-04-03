package com.planit.domain.trip.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.domain.trip.dto.GroupJoinResponse;
import com.planit.domain.trip.dto.GroupSubmitRequest;
import com.planit.domain.trip.dto.TripGroupStatusResponse;
import com.planit.domain.trip.entity.GroupMemberRole;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripGroup;
import com.planit.domain.trip.entity.TripGroupMember;
import com.planit.domain.trip.entity.TripStatus;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.entity.WantedPlace;
import com.planit.domain.trip.repository.TripGroupMemberRepository;
import com.planit.domain.trip.repository.TripGroupRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.repository.TripThemeRepository;
import com.planit.domain.trip.repository.WantedPlaceRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripGroupService {
    private static final int INVITE_TTL_HOURS = 24;
    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TripGroupRepository groupRepository;
    private final TripGroupMemberRepository groupMemberRepository;
    private final TripRepository tripRepository;
    private final TripThemeRepository tripThemeRepository;
    private final WantedPlaceRepository wantedPlaceRepository;
    private final UserRepository userRepository;
    private final TripAccessService tripAccessService;
    private final ItineraryEnqueueService itineraryEnqueueService;
    private final ObjectMapper objectMapper;

    @Transactional
    public String createWaitingGroup(Trip trip, User leader, Integer headCount, List<String> themes, List<String> wantedPlaces) {
        // 1) 그룹 여행 생성 시 리더를 즉시 submitted=true로 저장한다.
        //    따라서 submittedCount에는 생성 직후 리더 1명이 이미 포함된다.
        int normalizedHeadCount = headCount == null ? 0 : headCount;
        if (normalizedHeadCount < 2) {
            throw new BusinessException(ErrorCode.GROUP_003);
        }

        String inviteCode = generateUniqueInviteCode();
        TripGroup group = groupRepository.save(new TripGroup(
                trip,
                inviteCode,
                LocalDateTime.now().plusHours(INVITE_TTL_HOURS)
        ));

        trip.assignGroupId(group.getId());
        tripRepository.save(trip);

        TripGroupMember leaderMember = new TripGroupMember(
                group,
                leader,
                GroupMemberRole.LEADER,
                true,
                toJson(themes),
                toJson(wantedPlaces),
                LocalDateTime.now()
        );
        groupMemberRepository.save(leaderMember);

        return inviteCode;
    }

    @Transactional(readOnly = true)
    public GroupJoinResponse getJoinInfo(String inviteCode, String loginId) {
        // 2) join 화면 진입 시:
        //    - 리더가 처음 입력한 값(leader themes/wantedPlaces)
        //    - 현재 사용자 본인의 기존 입력값(my themes/wantedPlaces)
        //    를 함께 내려준다.
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        TripGroup group = getValidWaitingGroup(inviteCode);
        Trip trip = group.getTrip();

        TripGroupMember leaderMember = groupMemberRepository.findByGroupIdAndUserId(group.getId(), trip.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_001));
        Optional<TripGroupMember> myMember = groupMemberRepository.findByGroupIdAndUserId(group.getId(), user.getId());

        long submittedCount = groupMemberRepository.countByGroupIdAndSubmittedTrue(group.getId());

        return new GroupJoinResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getArrivalDate(),
                trip.getArrivalTime(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getTravelCity(),
                trip.getTotalBudget(),
                trip.getHeadCount(),
                submittedCount,
                group.getExpiresAt(),
                trip.getStatus(),
                fromJson(leaderMember.getThemesJson()),
                fromJson(leaderMember.getWantedPlacesJson()),
                myMember.map(m -> fromJson(m.getThemesJson())).orElse(List.of()),
                myMember.map(m -> fromJson(m.getWantedPlacesJson())).orElse(List.of()),
                myMember.map(TripGroupMember::isSubmitted).orElse(false)
        );
    }

    @Transactional
    public TripGroupStatusResponse submit(String inviteCode, String loginId, GroupSubmitRequest request) {
        // 3) 제출 요청 시 사용자 멤버 레코드를 찾고(없으면 생성), submitted=true + JSON 값으로 갱신한다.
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        TripGroup group = getValidWaitingGroup(inviteCode);
        Trip trip = group.getTrip();

        TripGroupMember member = groupMemberRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                .orElseGet(() -> groupMemberRepository.save(new TripGroupMember(
                        group,
                        user,
                        trip.getUser().getId().equals(user.getId()) ? GroupMemberRole.LEADER : GroupMemberRole.MEMBER,
                        false,
                        null,
                        null,
                        null
                )));

        member.submit(toJson(request.travelTheme()), toJson(request.wantedPlace()), LocalDateTime.now());
        groupMemberRepository.save(member);
        // 3-1) 그룹 멤버 제출값을 trip 레벨 테마/희망장소 테이블에도 동기화한다.
        //      (기존에는 trip_group_members에만 저장되어 그룹원 입력이 테이블에 반영되지 않았음)
        syncTripPreferencesBySubmittedMembers(group, trip);

        long submittedCount = groupMemberRepository.countByGroupIdAndSubmittedTrue(group.getId());
        // 4) 실제 일정 생성 트리거 조건:
        //    trip.status == WAITING && submittedCount >= headCount
        //    (조건을 만족해야만 멤버 입력값이 합쳐져 AI 생성 큐로 전달된다)
        if (trip.getStatus() == TripStatus.WAITING && trip.getHeadCount() != null && submittedCount >= trip.getHeadCount()) {
            trip.updateStatus(TripStatus.GENERATING);
            tripRepository.save(trip);
            enqueueBySubmittedMembers(group, trip);
        }

        return new TripGroupStatusResponse(
                trip.getId(),
                group.getInviteCode(),
                trip.getHeadCount(),
                submittedCount,
                trip.getStatus(),
                group.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public TripGroupStatusResponse getByTrip(Long tripId, String loginId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_001));
        tripAccessService.requireReadable(trip, loginId);

        TripGroup group = groupRepository.findByTripId(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_001));

        long submittedCount = groupMemberRepository.countByGroupIdAndSubmittedTrue(group.getId());
        return new TripGroupStatusResponse(
                trip.getId(),
                group.getInviteCode(),
                trip.getHeadCount(),
                submittedCount,
                trip.getStatus(),
                group.getExpiresAt()
        );
    }

    @Transactional
    public void cancelWaiting(Long tripId, String loginId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_001));
        TripAccessService.AccessInfo access = tripAccessService.requireReadable(trip, loginId);
        if (!access.isOwner()) {
            throw new BusinessException(ErrorCode.GROUP_004);
        }
        if (trip.getStatus() != TripStatus.WAITING) {
            throw new BusinessException(ErrorCode.GROUP_005);
        }
        trip.updateStatus(TripStatus.CANCELED);
        tripRepository.save(trip);
    }

    public boolean isMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    private void enqueueBySubmittedMembers(TripGroup group, Trip trip) {
        // 5) AI 요청에는 submitted=true 멤버의 값만 반영된다.
        //    아직 submit 안 한 멤버 값은 여기서 제외된다.
        List<TripGroupMember> members = groupMemberRepository.findByGroupIdAndSubmittedTrue(group.getId());
        Set<String> allThemes = new LinkedHashSet<>();
        Set<String> allWantedPlaces = new LinkedHashSet<>();

        for (TripGroupMember member : members) {
            allThemes.addAll(fromJson(member.getThemesJson()));
            allWantedPlaces.addAll(fromJson(member.getWantedPlacesJson()));
        }

        //잡큐잉 지점
        itineraryEnqueueService.enqueueGeneration(
                trip,
                new ArrayList<>(allThemes),
                new ArrayList<>(allWantedPlaces)
        );
    }

    private void syncTripPreferencesBySubmittedMembers(TripGroup group, Trip trip) {
        List<TripGroupMember> members = groupMemberRepository.findByGroupIdAndSubmittedTrue(group.getId());
        Set<String> allThemes = new LinkedHashSet<>();
        Set<String> allWantedPlaces = new LinkedHashSet<>();

        for (TripGroupMember member : members) {
            allThemes.addAll(fromJson(member.getThemesJson()));
            allWantedPlaces.addAll(fromJson(member.getWantedPlacesJson()));
        }

        tripThemeRepository.deleteByTripId(trip.getId());
        wantedPlaceRepository.deleteByTripId(trip.getId());

        for (String theme : allThemes) {
            tripThemeRepository.save(new TripTheme(trip, theme));
        }
        for (String wantedPlace : allWantedPlaces) {
            wantedPlaceRepository.save(new WantedPlace(trip, wantedPlace));
        }
    }

    private TripGroup getValidWaitingGroup(String inviteCode) {
        // 6) 입력 API는 WAITING 상태에서만 허용된다.
        //    이미 GENERATING/DONE 상태가 되면 submit/getJoinInfo에서 예외가 발생한다.
        TripGroup group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_001));
        if (group.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.GROUP_002);
        }
        if (group.getTrip().getStatus() != TripStatus.WAITING) {
            throw new BusinessException(ErrorCode.GROUP_005);
        }
        return group;
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomInviteCode(8);
            if (groupRepository.findByInviteCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique invite code");
    }

    private String randomInviteCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(INVITE_CHARS.length());
            builder.append(INVITE_CHARS.charAt(index));
        }
        return builder.toString();
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize group member values", ex);
        }
    }

    private List<String> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize group member values", ex);
        }
    }
}
