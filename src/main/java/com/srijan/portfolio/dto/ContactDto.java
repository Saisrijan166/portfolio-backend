package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactDto {
    private String primaryEmail;
    private List<ContactLinkDto> professionalLinks;
    private List<ContactLinkDto> socialLinks;
}
