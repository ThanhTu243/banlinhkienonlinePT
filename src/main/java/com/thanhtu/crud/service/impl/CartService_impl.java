package com.thanhtu.crud.service.impl;

import com.thanhtu.crud.entity.CartEntity;
import com.thanhtu.crud.entity.CustomerEntity;
import com.thanhtu.crud.entity.ProductEntity;
import com.thanhtu.crud.exception.NotEnoughQuantityException;
import com.thanhtu.crud.exception.NotFoundException;
import com.thanhtu.crud.model.dto.CartByCustomerDto;
import com.thanhtu.crud.model.dto.CartDto;
import com.thanhtu.crud.model.dto.fk.CartFKViewDto;
import com.thanhtu.crud.model.mapper.CartIdKeyMapper;
import com.thanhtu.crud.model.mapper.CartMapper;
import com.thanhtu.crud.model.request.CartRequest;
import com.thanhtu.crud.model.request.CartSelectRequest;
import com.thanhtu.crud.model.request.ProductSelectCartRequest;
import com.thanhtu.crud.repository.CartRepository;
import com.thanhtu.crud.repository.CustomerRepository;
import com.thanhtu.crud.repository.ProductRepository;
import com.thanhtu.crud.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CartService_impl implements CartService {
    @Autowired
    CartRepository cartRepo;
    @Autowired
    ProductRepository proRepo;
    @Autowired
    CustomerRepository customerRepo;
    @Override
    public CartDto createCart(CartRequest cartRequest) {
        CustomerEntity customer=customerRepo.findCustomerEntityByCustomerIdAndIsDelete(cartRequest.getCustomerId(),"NO");
        ProductEntity product=proRepo.findProductEntityByProductIdAndIsDelete(cartRequest.getProductId(), "NO");
        CartEntity cart=cartRepo.findCartEntitiesByCustomerEntityAndProductEntityAndIsDelete(customer,product,"NO");

        if(product==null)
        {
            throw new NotFoundException("Không tìm thấy sản phẫm có id: "+cartRequest.getProductId());
        }
        if(cartRequest.getQuantity()> product.getQuantity())
        {
            throw new NotEnoughQuantityException("Không đủ số lượng. Sản phẫm chỉ còn "+product.getQuantity());
        }

        if(cart==null)
        {
            cartRepo.save(CartMapper.toCartEntity(cartRequest,product,customer));
            return null;
        }
        else{
            int quantityUpdate=cart.getQuantity()+cartRequest.getQuantity();
            cart.setQuantity(quantityUpdate);
            cartRepo.save(cart);
            return CartMapper.toCartDto(cart);
        }

    }

    @Override
    public CartDto updateCart(CartRequest cartRequest) {
        CustomerEntity customer=customerRepo.findCustomerEntityByCustomerIdAndIsDelete(cartRequest.getCustomerId(),"NO");
        ProductEntity product=proRepo.findProductEntityByProductIdAndIsDelete(cartRequest.getProductId(), "NO");
        CartEntity cart=cartRepo.findCartEntitiesByCustomerEntityAndProductEntityAndIsDelete(customer,product,"NO");
        if(cart==null)
        {
            throw new NotFoundException("Không tìm thấy sản phẫm có id: "+cartRequest.getProductId());
        }
        if(cartRequest.getQuantity()> product.getQuantity())
        {
            throw new NotEnoughQuantityException("Không đủ số lượng. Sản phẫm chỉ còn "+product.getQuantity());
        }

        cart.setQuantity(cartRequest.getQuantity());
        cartRepo.save(cart);
        return CartMapper.toCartDto(cart);
    }

    @Override
    public CartDto deleteCart(CartRequest cartRequest) {
        CustomerEntity customer=customerRepo.getById(cartRequest.getCustomerId());
        ProductEntity product=proRepo.findProductEntityByProductIdAndIsDelete(cartRequest.getProductId(), "NO");
        CartEntity cart=cartRepo.findCartEntitiesByCustomerEntityAndProductEntityAndIsDelete(customer,product,"NO");
        if(cart==null)
        {
            throw new NotFoundException("Không có mặt hàng này trong giỏ");
        }
        cartRepo.delete(cart);
        return CartMapper.toCartDto(cart);
    }

    @Override
    public int deleteCartById(int id) {
        CartEntity cart=cartRepo.findCartEntitiesByCartIdAndIsDelete(id,"NO");
        cart.setIsDelete("YES");
        cartRepo.save(cart);
        return 1;
    }

    @Override
    public CartByCustomerDto getCartByCustomer(int customerId) {
        CustomerEntity customerEntity= customerRepo.findCustomerEntityByCustomerIdAndIsDelete(customerId,"NO");
        if(customerEntity==null)
        {
            throw new NotFoundException("Không tìm thấy khách hàng có Id: "+customerId);
        }
        List<CartEntity> listCart=cartRepo.findCartEntityByCustomerEntityAndIsDelete(customerEntity,"NO");
        CartByCustomerDto cartByCustomerDto=new CartByCustomerDto();
        cartByCustomerDto.setCustomerId(customerId);
        cartByCustomerDto.setUserCustomer(customerEntity.getUserCustomer());
        cartByCustomerDto.setFullnameCustomer(customerEntity.getFirstnameCustomer()+" "+customerEntity.getLastnameCustomer());
        cartByCustomerDto.setGmailCustomer(customerEntity.getGmailCustomer());
        cartByCustomerDto.setPhoneNumberCustomer(customerEntity.getPhonenumberCustomer());
        cartByCustomerDto.setAddress(customerEntity.getAddress());
        Set<CartFKViewDto> set=new HashSet<CartFKViewDto>();
        for(CartEntity cart:listCart)
        {
            ProductEntity product= proRepo.findProductEntityByProductIdAndIsDelete(cart.getProductEntity().getProductId(),"NO");
            set.add(CartMapper.toCartFKViewDto(product,cart));
        }
        cartByCustomerDto.setCartEntities(set);
        return cartByCustomerDto;
    }

    @Override
    public Long selectCart(CartSelectRequest cartSelectRequest) {
        CustomerEntity customer= customerRepo.findCustomerEntityByCustomerIdAndIsDelete(cartSelectRequest.getCustomerId(),"NO");
        if(customer==null)
        {
            throw new NotFoundException("Không tìm được khách hàng có Id: "+cartSelectRequest.getCustomerId());
        }
        Long sumCart=Long.valueOf(0);
        List<ProductSelectCartRequest> list=cartSelectRequest.getListProducts();
        for(ProductSelectCartRequest productSelectCartRequest:list)
        {
            ProductEntity product= proRepo.findProductEntityByProductIdAndIsDelete(productSelectCartRequest.getProductId(),"No");
            if(product==null)
            {
                throw new NotFoundException("Không tìm thấy sản phẫm có Id "+productSelectCartRequest.getProductId());
            }
            CartEntity cart=cartRepo.findCartEntitiesByCustomerEntityAndProductEntityAndIsDelete(customer,product,"NO");
            if(cart==null)
            {
                throw new NotFoundException("Không có trong giỏ hàng");
            }
            Long priceAfterDiscount=Long.valueOf(product.getUnitPrice()*(100-product.getQuantity())/100);
            sumCart+=Long.valueOf(priceAfterDiscount* productSelectCartRequest.getQuantity());
        }
        return sumCart;
    }
}
