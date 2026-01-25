package com.mayday.crawler.service.impl;

import com.mayday.crawler.mapper.CmsCategoryMapper;
import com.mayday.crawler.modl.entity.CmsCategoryEntity;
import com.mayday.crawler.service.ICmsCategoryService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CmsCategoryServiceImpl extends ServiceImpl<CmsCategoryMapper, CmsCategoryEntity> implements ICmsCategoryService {
}
