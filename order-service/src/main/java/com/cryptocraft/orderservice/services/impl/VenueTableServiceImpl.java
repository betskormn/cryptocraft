package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.controllers.dto.request.VenueTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.VenueTableResponseDto;
import com.cryptocraft.orderservice.dao.VenueTable;
import com.cryptocraft.orderservice.exceptions.OrderRejectedException;
import com.cryptocraft.orderservice.mappers.VenueTableMapper;
import com.cryptocraft.orderservice.repositories.VenueTableRepository;
import com.cryptocraft.orderservice.services.VenueTableService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenueTableServiceImpl implements VenueTableService {

    // QR code token: unguessable, no look-alike characters (0/O, 1/I/L) for when staff read it out
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VenueTableRepository venueTableRepository;
    private final VenueTableMapper venueTableMapper;

    @Override
    @Transactional
    public VenueTableResponseDto createTable(VenueTableRequestDto requestDto) {
        String label = requestDto.label().trim();
        if (venueTableRepository.existsByLabelIgnoreCase(label)) {
            throw new OrderRejectedException(format("Table '%s' already exists", label));
        }
        VenueTable table = VenueTable.builder()
                .label(label)
                .code(newCode())
                .active(Objects.requireNonNullElse(requestDto.active(), true))
                .build();
        VenueTable saved = venueTableRepository.save(table);
        log.info("Table with id:{}, label:{} successfully created", saved.getTableId(), saved.getLabel());
        return venueTableMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueTableResponseDto> getAllTables() {
        return venueTableRepository.findAllByOrderByLabelAsc().stream()
                .map(venueTableMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public VenueTableResponseDto updateTable(Long id, VenueTableRequestDto requestDto) {
        VenueTable table = venueTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Table with id:%s, not founded", id)));
        String label = requestDto.label().trim();
        if (venueTableRepository.existsByLabelIgnoreCaseAndTableIdNot(label, id)) {
            throw new OrderRejectedException(format("Table '%s' already exists", label));
        }
        table.setLabel(label);
        table.setActive(Objects.requireNonNullElse(requestDto.active(), table.isActive()));
        log.info("Table with id:{} successfully updated", id);
        return venueTableMapper.toDto(venueTableRepository.save(table));
    }

    private static String newCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }
}
