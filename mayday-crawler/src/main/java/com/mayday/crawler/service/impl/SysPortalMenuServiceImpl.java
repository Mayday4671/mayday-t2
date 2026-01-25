package com.mayday.crawler.service.impl;

import com.mayday.crawler.mapper.SysPortalMenuMapper;
import com.mayday.crawler.modl.entity.SysPortalMenuEntity;
import com.mayday.crawler.service.ISysPortalMenuService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysPortalMenuServiceImpl extends ServiceImpl<SysPortalMenuMapper, SysPortalMenuEntity> implements ISysPortalMenuService {
}
