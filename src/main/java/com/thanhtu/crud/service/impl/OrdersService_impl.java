package com.thanhtu.crud.service.impl;


import com.thanhtu.crud.entity.*;
import com.thanhtu.crud.exception.NotFoundException;
import com.thanhtu.crud.model.dto.*;
import com.thanhtu.crud.model.mapper.CartIdKeyMapper;
import com.thanhtu.crud.model.mapper.OrdersDetailMapper;
import com.thanhtu.crud.model.mapper.OrdersMapper;
import com.thanhtu.crud.model.request.*;
import com.thanhtu.crud.repository.*;
import com.thanhtu.crud.service.CartService;
import com.thanhtu.crud.service.OrdersService;
import com.thanhtu.crud.service.email.MailSender;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
@RequiredArgsConstructor
public class OrdersService_impl implements OrdersService {
    @Autowired
    PaypalService_impl paypalService;
    @Autowired
    OrdersRepository orderRepo;
    @Autowired
    OrdersDetailRepository ordersDetailRepo;
    @Autowired
    ProductRepository productRepo;
    @Autowired
    CartRepository cartRepo;
    @Autowired CustomerRepository customerRepo;
    private final MailSender mailSender;


    @Override
    public Page<OrdersEntity> getListOrderByStatus(String status, Pageable page) {
        return orderRepo.findOrdersEntityByStatusOrderOrderByCreateDateDesc(status,page);
    }

    @Override
    public OrdersDto updateOrders(int id, OrdersUpdateRequest ordersUpdateRequest) {
        OrdersEntity ordersDelete=orderRepo.findOrdersEntityByOrderIdAndStatusOrder(id,"Đã hủy");
        if(ordersDelete!=null)
        {
            throw new NotFoundException("Đã hủy đơn hàng với id: "+id);
        }
        OrdersEntity ordersEntity=orderRepo.findById(id).orElseThrow(()-> new NotFoundException("Không tồn tại đơn hàng với id: "+id));
        OrdersEntity orders=orderRepo.save(OrdersMapper.toUpdateOrders(ordersEntity,ordersUpdateRequest));
        return OrdersMapper.toOrdersDto(orders);
    }

    @Override
    public void approvalOrders(OrdersUpdateStatusRequest ordersUpdateStatusRequest) {
        for(OrdersIdDto orderId:ordersUpdateStatusRequest.getList())
        {
            OrdersEntity ordersEntity=orderRepo.findById(orderId.getOrdersId()).orElseThrow(()-> new NotFoundException("Không tồn tại đơn hàng với id: "+orderId.getOrdersId()));
            ordersEntity.setStatusOrder("Đã duyệt");
            orderRepo.save(ordersEntity);
        }
    }

    @Override
    public List<OrdersEntity> getListOrderByStatus(String status) {
        List<OrdersEntity> list=orderRepo.findOrdersEntitiesByStatusOrderOrderByCreateDate(status);
        return list;
    }

    @Override
    public void cancelOrder(Integer id) {
        OrdersEntity order=orderRepo.findOrdersEntityByOrderId(id);
        if(order==null)
        {
            throw new NotFoundException("Không tìm thấy đơn hàng có id: "+id);
        }
        order.setStatusOrder("Đã hủy");
        List<OrderDetailEntity> listCancel= ordersDetailRepo.findOrderDetailEntityByOrdersEntity(order);
        for(OrderDetailEntity orderDetail:listCancel)
        {
            orderDetail.setIsDelete("YES");
            ProductEntity product= productRepo.findProductEntityByProductIdAndIsDelete(orderDetail.getId().getProductId(),"NO");
            if(product!=null)
            {
                int quantityUpdate=product.getQuantity()+orderDetail.getQuantity();
                product.setQuantity(quantityUpdate);
                productRepo.save(product);
            }
            ordersDetailRepo.save(orderDetail);
        }
        orderRepo.save(order);
    }

    @Override
    public OrdersDto createOrders(OrderCreateRequest orderCreateRequest) {
        List<Integer> cartIdList=orderCreateRequest.getCartItemList();
        List<CartEntity> listCartItem=new ArrayList<>();
        for(Integer cartIdItem:cartIdList)
        {
            listCartItem.add(cartRepo.findCartEntitiesByCartIdAndIsDelete(cartIdItem,"NO"));
        }
        for(CartEntity cartEntity:listCartItem)
        {
            ProductEntity product=cartEntity.getProductEntity();
            if(cartEntity.getProductEntity().getIsDelete().equals("YES"))
            {
                throw new NotFoundException(""+product.getProductName()+" không còn bán nữa");
            }
            if(cartEntity.getQuantity()>cartEntity.getProductEntity().getQuantity())
            {
                throw new NotFoundException(""+product.getProductName()+" không đủ số lượng. Chỉ còn "+product.getQuantity());
            }
        }
        CustomerEntity customer=listCartItem.get(0).getCustomerEntity();
        OrdersEntity orderCreate=orderRepo.save(OrdersMapper.toCreateOrders(orderCreateRequest,customer,"COD"));
        for(CartEntity cartEntity:listCartItem)
        {
            cartEntity.setIsDelete("YES");
            cartRepo.save(cartEntity);
            ordersDetailRepo.save(OrdersDetailMapper.toOrderDetailEntity(cartEntity,orderCreate));
            ProductEntity product=cartEntity.getProductEntity();
            int quantityUpdate=product.getQuantity()-cartEntity.getQuantity();
            product.setQuantity(quantityUpdate);
            productRepo.save(product);
        }
        String subject = "Đơn hàng #" + orderCreate.getOrderId();
        String template = "order-template";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("order", orderCreate);
        attributes.put("fullname",customer.getFirstnameCustomer()+" "+customer.getLastnameCustomer());
        mailSender.sendMessageHtml(orderCreate.getCustomerEntity().getGmailCustomer(), subject, template, attributes);
        return OrdersMapper.toOrdersDto(orderCreate);
    }
    @Override
    public OrdersDto createOrdersOnline(OrderCreateRequest orderCreateRequest,String methodPayment)
    {
        List<Integer> cartIdList=orderCreateRequest.getCartItemList();
        List<CartEntity> listCartItem=new ArrayList<>();
        for(Integer cartIdItem:cartIdList)
        {
            listCartItem.add(cartRepo.findCartEntitiesByCartIdAndIsDelete(cartIdItem,"NO"));
        }
        for(CartEntity cartEntity:listCartItem)
        {
            ProductEntity product=cartEntity.getProductEntity();
            if(cartEntity.getProductEntity().getIsDelete().equals("YES"))
            {
                throw new NotFoundException(""+product.getProductName()+" không còn bán nữa");
            }
            if(cartEntity.getQuantity()>cartEntity.getProductEntity().getQuantity())
            {
                throw new NotFoundException(""+product.getProductName()+" không đủ số lượng. Chỉ còn "+product.getQuantity());
            }
        }
        CustomerEntity customer=listCartItem.get(0).getCustomerEntity();
        OrdersEntity orderCreate=orderRepo.save(OrdersMapper.toCreateOrders(orderCreateRequest,customer,methodPayment));
        for(CartEntity cartEntity:listCartItem)
        {
            cartEntity.setIsDelete("YES");
            cartRepo.save(cartEntity);
            ordersDetailRepo.save(OrdersDetailMapper.toOrderDetailEntity(cartEntity,orderCreate));
            ProductEntity product=cartEntity.getProductEntity();
            int quantityUpdate=product.getQuantity()-cartEntity.getQuantity();
            product.setQuantity(quantityUpdate);
            productRepo.save(product);
        }
        return OrdersMapper.toOrdersDto(orderCreate);
    }

    @Override
    public void confirmPaymentAndSendMail(int orderId) {
        OrdersEntity ordersEntity=orderRepo.findOrdersEntityByOrderId(orderId);
        ordersEntity.setPaymentStatus("Đã thanh toán");
        orderRepo.save(ordersEntity);
        String subject = "Đơn hàng #" + orderId;
        String template = "order-template";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("order", ordersEntity);
        attributes.put("fullname",ordersEntity.getCustomerEntity().getFirstnameCustomer()+" "+ordersEntity.getCustomerEntity().getLastnameCustomer());
        mailSender.sendMessageHtml(ordersEntity.getCustomerEntity().getGmailCustomer(), subject, template, attributes);
    }

    @Override
    public void sendEmailOrder() {

    }

    @Override
    public List<OrderDetailView> getOrderDetailByCustomerIdAndStatus(int id,OrdersStatusRequest ordersStatusRequest) {
        List<OrderDetailView> orderDetailViewList=new ArrayList<OrderDetailView>();
        CustomerEntity customer=customerRepo.findCustomerEntityByCustomerIdAndIsDelete(id,"NO");
        List<OrdersEntity> ordersEntities=orderRepo.findOrdersEntityByCustomerEntityAndStatusOrderOrderByCreateDateDesc(customer,ordersStatusRequest.getStatusOrder());
        for(OrdersEntity ordersEntity:ordersEntities)
        {
            OrderDetailView orderDetailView= new OrderDetailView();
            orderDetailView.setOrderId(ordersEntity.getOrderId());
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("E, MMM dd yyyy HH:mm:ss");
            String formattedDate = ordersEntity.getCreateDate().toLocalDateTime().format(myFormatObj);
            orderDetailView.setCreateDate(formattedDate);
            orderDetailView.setAmount(ordersEntity.getTotalAmount());
            List<OrderDetailEntity> listOrderDetail=ordersDetailRepo.findOrderDetailEntityByOrdersEntity(ordersEntity);
            List<ProductOrder> listProductOrder=new ArrayList<ProductOrder>();
            for(OrderDetailEntity orderDetailEntity:listOrderDetail)
            {
                ProductOrder productOrder=new ProductOrder();
                productOrder.setOrderId(orderDetailEntity.getId().getOrderId());
                productOrder.setProducId(orderDetailEntity.getId().getProductId());
                Set<ImageEntity> listimage=orderDetailEntity.getProductEntity().getImageEntities();
                String productImage="";
                for(ImageEntity imageEntity:listimage)
                {
                    productImage=imageEntity.getImage();
                }
                productOrder.setProductImage(productImage);
                productOrder.setProductName(orderDetailEntity.getProductEntity().getProductName());
                productOrder.setQuantity(orderDetailEntity.getQuantity());
                listProductOrder.add(productOrder);
            }
            orderDetailView.setListProduct(listProductOrder);
            orderDetailViewList.add(orderDetailView);
        }
        return orderDetailViewList;
    }

    @Override
    public void orderDelivered(OrdersUpdateStatusRequest ordersUpdateStatusRequest) {
        for(OrdersIdDto orderId:ordersUpdateStatusRequest.getList())
        {
            OrdersEntity ordersEntity=orderRepo.findById(orderId.getOrdersId()).orElseThrow(()-> new NotFoundException("Không tồn tại đơn hàng với id: "+orderId.getOrdersId()));
            ordersEntity.setStatusOrder("Đã giao");
            ordersEntity.setNote("Đã thanh toán");
            orderRepo.save(ordersEntity);
        }
    }

    @Override
    public List<ProductToReview> getOrderDetailByCustomerToReview(int id, String statusOrder) {
        List<ProductToReview> productToReviewList=new ArrayList<ProductToReview>();
        CustomerEntity customer=customerRepo.findCustomerEntityByCustomerIdAndIsDelete(id,"NO");
        List<OrdersEntity> ordersEntities=orderRepo.findOrdersEntityByCustomerEntityAndStatusOrderOrderByCreateDateDesc(customer,statusOrder);
        for(OrdersEntity ordersEntity:ordersEntities)
        {
            List<OrderDetailEntity> listOrderDetail=ordersDetailRepo.findOrderDetailEntityByOrdersEntity(ordersEntity);
            for(OrderDetailEntity orderDetailEntity:listOrderDetail)
            {
                ProductToReview productToReview= new ProductToReview();
                productToReview.setOrderId(ordersEntity.getOrderId());
                DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("E, MMM dd yyyy HH:mm:ss");
                String formattedDate = ordersEntity.getCreateDate().toLocalDateTime().format(myFormatObj);
                productToReview.setCreateOrders(formattedDate);
                productToReview.setProductId(orderDetailEntity.getId().getProductId());
                Set<ImageEntity> listimage=orderDetailEntity.getProductEntity().getImageEntities();
                String productImage="";
                for(ImageEntity imageEntity:listimage)
                {
                    productImage=imageEntity.getImage();
                }
                productToReview.setProductImage(productImage);
                productToReview.setProductName(orderDetailEntity.getProductEntity().getProductName());
                productToReview.setQuantity(orderDetailEntity.getQuantity());
                productToReview.setIsReview(orderDetailEntity.getIsReview());
                productToReviewList.add(productToReview);
            }
        }
        Collections.sort(productToReviewList, new Comparator<ProductToReview>() {
            @Override
            public int compare(ProductToReview o1, ProductToReview o2) {
                if(o1.getIsReview().equals("NO") && o2.getIsReview().equals("YES"))
                {
                    return -1;
                }
                else if(o1.getIsReview().equals("NO") && o2.getIsReview().equals("YES")){
                    return 1;
                }
                else{
                    if(o1.getOrderId()< o2.getOrderId())
                    {
                        return 1;
                    }
                    else{
                        return 0;
                    }
                }
            }
        });
        return productToReviewList;
    }

    @Override
    public OrderStatistic getStasticOrder(int customerId) {
        CustomerEntity customer=customerRepo.findCustomerEntityByCustomerIdAndIsDelete(customerId,"NO");
        OrderStatistic orderStatistic=new OrderStatistic();
        orderStatistic.setQuantityOfCancelOrder(orderRepo.countAllByCustomerEntityAndStatusOrder(customer,"Đã hủy"));
        orderStatistic.setQuantityOfApprovedOrder(orderRepo.countAllByCustomerEntityAndStatusOrder(customer,"Đã duyệt"));
        orderStatistic.setQuantityOfDeliveredOrder(orderRepo.countAllByCustomerEntityAndStatusOrder(customer,"Đã giao"));
        orderStatistic.setQuantityOfTheOrderBeApprove(orderRepo.countAllByCustomerEntityAndStatusOrder(customer,"Chưa duyệt"));
        return orderStatistic;
    }

    @Override
    public void callBackOrder(Integer orderId, List<Integer> cartItemList) {
        for(Integer cartId:cartItemList)
        {
            CartEntity cart=cartRepo.findCartEntitiesByCartIdAndIsDelete(cartId,"YES");
            cart.setIsDelete("NO");
            cartRepo.save(cart);
        }
        OrdersEntity ordersEntity=orderRepo.findOrdersEntityByOrderId(orderId);
        ordersEntity.setStatusOrder("Đã xóa");
    }

}
