package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.TenantRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.request.TenantUpdateRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.TenantResponseDto;
import com.cryptocraft.inventoryservice.dao.Tenant;
import com.cryptocraft.tenant.TenantStatus;
import com.cryptocraft.inventoryservice.exceptions.TenantAlreadyExistsException;
import com.cryptocraft.inventoryservice.mappers.TenantMapper;
import com.cryptocraft.inventoryservice.repositories.TenantCatalogRepository;
import com.cryptocraft.inventoryservice.repositories.TenantRepository;
import com.cryptocraft.inventoryservice.services.TenantService;
import com.cryptocraft.inventoryservice.tenant.TenantRegistry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    public static final String DEMO_TEMPLATE_SLUG = "demo-template";

    private static final String DEFAULT_CURRENCY = "UAH";
    private static final String DEFAULT_TIMEZONE = "Europe/Kyiv";
    private static final int DEFAULT_PRICE_TICK_SECONDS = 30;

    private final TenantRepository tenantRepository;
    private final TenantCatalogRepository tenantCatalogRepository;
    private final TenantMapper tenantMapper;
    private final TenantRegistry tenantRegistry;

    @Override
    @Transactional
    public TenantResponseDto createTenant(TenantRequestDto requestDto) {
        String name = requestDto.name().trim();
        String slug = requestDto.slug();
        if (tenantRepository.existsBySlug(slug)) {
            throw new TenantAlreadyExistsException(format("Tenant with slug '%s' already exists", slug));
        }
        if (tenantRepository.existsByNameIgnoreCase(name)) {
            throw new TenantAlreadyExistsException(format("Tenant with name '%s' already exists", name));
        }

        Tenant tenant = Tenant.builder()
                .name(name)
                .slug(slug)
                .currency(Objects.requireNonNullElse(requestDto.currency(), DEFAULT_CURRENCY))
                .timezone(validTimezone(Objects.requireNonNullElse(requestDto.timezone(), DEFAULT_TIMEZONE)))
                .priceTickSeconds(Objects.requireNonNullElse(requestDto.priceTickSeconds(), DEFAULT_PRICE_TICK_SECONDS))
                .status(TenantStatus.ACTIVE)
                .build();
        // flushed before the catalog copy, which references the new tenant row
        Tenant saved = tenantRepository.saveAndFlush(tenant);

        if (Boolean.TRUE.equals(requestDto.demoMenu())) {
            Tenant template = tenantRepository.findBySlug(DEMO_TEMPLATE_SLUG)
                    .orElseThrow(() -> new IllegalStateException(
                            format("Demo template tenant '%s' is missing", DEMO_TEMPLATE_SLUG)));
            tenantCatalogRepository.cloneCatalog(template.getTenantId(), saved.getTenantId());
            log.info("Demo menu copied from tenant id:{} to tenant id:{}", template.getTenantId(), saved.getTenantId());
        }

        log.info("Tenant with id:{}, slug:{} successfully created", saved.getTenantId(), saved.getSlug());
        return tenantMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponseDto> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(tenantMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponseDto> getTenantsByIds(Collection<Long> ids) {
        return tenantRepository.findAllById(ids)
                .stream()
                .sorted(Comparator.comparing(Tenant::getName, String.CASE_INSENSITIVE_ORDER))
                .map(tenantMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponseDto getTenantById(Long id) {
        return tenantMapper.toDto(findTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponseDto getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(format("Tenant with slug:%s, not founded", slug)));
        return tenantMapper.toDto(tenant);
    }

    @Override
    @Transactional
    public TenantResponseDto updateTenant(Long id, TenantUpdateRequestDto requestDto) {
        Tenant tenant = findTenant(id);
        String name = requestDto.name().trim();
        if (!tenant.getName().equalsIgnoreCase(name) && tenantRepository.existsByNameIgnoreCase(name)) {
            throw new TenantAlreadyExistsException(format("Tenant with name '%s' already exists", name));
        }
        tenant.setName(name);
        tenant.setCurrency(requestDto.currency());
        tenant.setTimezone(validTimezone(requestDto.timezone()));
        tenant.setPriceTickSeconds(requestDto.priceTickSeconds());
        tenant.setStatus(requestDto.status());
        Tenant saved = tenantRepository.save(tenant);
        // the request filter caches the status, so a suspension must take effect immediately
        tenantRegistry.evict(id);
        log.info("Tenant with id:{} successfully updated", id);
        return tenantMapper.toDto(saved);
    }

    private Tenant findTenant(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Tenant with id:%s, not founded", id)));
    }

    private static String validTimezone(String timezone) {
        try {
            return ZoneId.of(timezone).getId();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(format("Unknown timezone: %s", timezone));
        }
    }
}
