package cn.edu.csu.mycoursestore.cart.mapper;

import cn.edu.csu.mycoursestore.cart.entity.CartItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}