package by.teachmeskills.springbootproject.services.impl;

import by.teachmeskills.springbootproject.entities.Cart;
import by.teachmeskills.springbootproject.entities.Category;
import by.teachmeskills.springbootproject.entities.Product;
import by.teachmeskills.springbootproject.enums.PagesPathEnum;
import by.teachmeskills.springbootproject.exceptions.DBConnectionException;
import by.teachmeskills.springbootproject.repositories.ProductRepository;
import by.teachmeskills.springbootproject.repositories.impl.ProductRepositoryImpl;
import by.teachmeskills.springbootproject.services.ProductService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepositoryImpl productRepository) {
        this.productRepository = productRepository;
    }


    @Override
    public List<Product> read() throws DBConnectionException {
        return productRepository.read();
    }

    @Override
    public void create(Product product) throws DBConnectionException {
        productRepository.create(product);

    }

    @Override
    public void delete(int id) throws DBConnectionException {
        productRepository.delete(id);
    }

    @Override
    public Product findById(int id) throws DBConnectionException {
        return productRepository.findById(id);
    }

    @Override
    public ModelAndView getProductsByCategory(int id) throws DBConnectionException {
        ModelMap modelMap = new ModelMap();
        modelMap.addAttribute("categoryProducts", productRepository.getProductsByCategory(id));
        return new ModelAndView(PagesPathEnum.CATEGORY_PAGE.getPath(), modelMap);
    }

    @Override
    public ModelAndView addProductToCart(int productId, Cart cart) throws DBConnectionException {
        ModelMap modelMap = new ModelMap();
        Product product = findById(productId);
        cart.addProduct(product);
        modelMap.addAttribute("cart", cart);
        modelMap.addAttribute("product", product);
        return new ModelAndView(PagesPathEnum.PRODUCT_PAGE.getPath(), modelMap);
    }

    @Override
    public ModelAndView deleteProductFromCart(int productId, Cart cart) {
        ModelMap modelMap = new ModelMap();
        cart.removeProduct(productId);
        modelMap.addAttribute("cart", cart);
        if (cart.getProducts().isEmpty()) {
            return new ModelAndView(PagesPathEnum.EMPTY_CART_PAGE.getPath());
        }
        return new ModelAndView(PagesPathEnum.CART_PAGE.getPath(), modelMap);
    }

    @Override
    public ModelAndView clearCart(Cart cart) {
        cart.clear();
        ModelMap modelMap = new ModelMap();
        if (cart.getProducts().isEmpty()) {
            return new ModelAndView(PagesPathEnum.EMPTY_CART_PAGE.getPath());
        }
        return new ModelAndView(PagesPathEnum.CART_PAGE.getPath(), modelMap.addAttribute("cart", cart));
    }


    @Override
    public ModelAndView findProductByIdForProductPage(int id) throws DBConnectionException {
        ModelMap modelMap = new ModelMap();
        Product product = findById(id);
        modelMap.addAttribute("categoryName", product.getName());
        modelMap.addAttribute("product", product);
        return new ModelAndView(PagesPathEnum.PRODUCT_PAGE.getPath(), modelMap);
    }

    @Override
    public ModelAndView searchProductsPaged(int pageNumber, String keyWords) throws DBConnectionException {
        Long totalRecords;
        List<Product> products;
        int pageMaxResult;
        ModelMap modelMap = new ModelMap();
        if (keyWords != null) {
            totalRecords = productRepository.findProductsQuantityByKeywords(keyWords);
            pageMaxResult = (int) (totalRecords / 3);
            products = productRepository.findProductsByKeywords(keyWords, pageNumber, pageMaxResult);
            modelMap.addAttribute("products", products);
        }
        return new ModelAndView(PagesPathEnum.SEARCH_PAGE.getPath(), modelMap);
    }

    @Override
    public ModelAndView saveProductsFromFile(MultipartFile file, int id) throws DBConnectionException {
        List<Product> csvProducts = parseCsv(file);
        ModelMap modelMap = new ModelMap();
        if (Optional.ofNullable(csvProducts).isPresent()) {
            for (Product csvProduct : csvProducts) {
                productRepository.create(csvProduct);
            }
            modelMap.addAttribute("categoryProducts", productRepository.getProductsByCategory(id));
            return new ModelAndView(PagesPathEnum.CATEGORY_PAGE.getPath(), modelMap);
        }
        return new ModelAndView(PagesPathEnum.CATEGORY_PAGE.getPath(), modelMap);
    }

    private List<Product> parseCsv(MultipartFile file) {
        if (Optional.ofNullable(file).isPresent()) {
            try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                CsvToBean<Product> csvToBean = new CsvToBeanBuilder<Product>(reader).withType(Product.class).
                        withIgnoreLeadingWhiteSpace(true).withSeparator(';').build();
                return csvToBean.parse();
            } catch (IOException e) {
                log.error("Exception occurred during csv parsing:" + e.getMessage());
            }
        } else {
            log.error("Empty scv file is uploaded");
        }
        return Collections.emptyList();
    }

    @Override
    public void saveCategoryProductsToFile(HttpServletResponse servletResponse) throws DBConnectionException, IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        List<Product> products = productRepository.read();
        try (Writer writer = new OutputStreamWriter(servletResponse.getOutputStream())) {
            StatefulBeanToCsv<Product> beanToCsv = new StatefulBeanToCsvBuilder<Product>(writer).withSeparator(';').build();
            servletResponse.setContentType("text/csv");
            servletResponse.addHeader("Content-Disposition", "attachment; filename=" + "products.csv");
            beanToCsv.write(products);
        }
    }
}
