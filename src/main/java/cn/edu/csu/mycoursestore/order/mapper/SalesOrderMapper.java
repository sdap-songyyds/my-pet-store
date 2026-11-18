package cn.edu.csu.mycoursestore.order.mapper;

import cn.edu.csu.mycoursestore.order.entity.SalesOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
}